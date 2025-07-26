package org.setms.sew.core.inbound.tool;

import static org.setms.sew.core.inbound.tool.Inputs.acceptanceTests;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Output;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.sew.core.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.sew.core.domain.model.sdlc.acceptance.ElementVariable;
import org.setms.sew.core.domain.model.sdlc.acceptance.Scenario;

public class AcceptanceTestTool extends BaseTool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(acceptanceTests());
  }

  @Override
  public Optional<Output> getOutputs() {
    return Optional.empty();
  }

  @Override
  protected void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var acceptanceTests = inputs.get(AcceptanceTest.class);
    var reportResource = resource.select("reports/acceptanceTests");
    acceptanceTests.forEach(acceptanceTest -> build(acceptanceTest, reportResource, diagnostics));
  }

  private void build(
      AcceptanceTest acceptanceTest, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var report =
        resource.select(
            "%s-%s.html"
                .formatted(acceptanceTest.getSut().getId(), acceptanceTest.getSut().getType()));
    try (var writer = new PrintWriter(report.writeTo())) {
      writer.println("<html>");
      writer.println("  <body>");
      writer.printf(
          "    <h1>Acceptance tests for %s %s</h1>%n",
          acceptanceTest.getSut().getType(), acceptanceTest.getSut().getId());
      acceptanceTest.getScenarios().forEach(scenario -> build(scenario, acceptanceTest, writer));
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private void build(Scenario scenario, AcceptanceTest acceptanceTest, PrintWriter writer) {
    var name = acceptanceTest.getSut().getId();
    writer.printf("    <h2>%s</h2>%n", scenario.getName());
    writer.printf(
        "    <strong>Given</strong> <code>%s</code> %s<br/>%n",
        name, stateOf(scenario.getInit(), acceptanceTest));
    writer.printf(
        "    <strong>When</strong> <code>%s</code> accepts <code>%s</code><br/>%n",
        name, format(scenario.getCommand(), acceptanceTest).orElse("?"));
    writer.printf(
        "    <strong>Then</strong> <code>%s</code> %s<br/>%n",
        name, stateOf(scenario.getState(), acceptanceTest));
    writer.printf(
        "    <strong>And</strong> <code>%s</code> emits <code>%s</code><br/>%n",
        name, format(scenario.getEmitted(), acceptanceTest).orElse("?"));
  }

  private String stateOf(Link state, AcceptanceTest acceptanceTest) {
    return format(state, acceptanceTest).map("contains %s"::formatted).orElse("is empty");
  }

  private Optional<String> format(Link linkToVariable, AcceptanceTest acceptanceTest) {
    return Optional.ofNullable(linkToVariable)
        .flatMap(acceptanceTest::findVariable)
        .map(ElementVariable.class::cast)
        .map(this::format);
  }

  private String format(ElementVariable variable) {
    var result = new StringBuilder("%s{".formatted(variable.getType().getId()));
    var prefix = new AtomicReference<>("");
    Stream.ofNullable(variable.getDefinitions())
        .flatMap(Collection::stream)
        .forEach(
            definition ->
                result
                    .append(prefix.getAndSet(", "))
                    .append(" $")
                    .append(definition.getFieldName()));
    return result.append(" }").toString();
  }
}
