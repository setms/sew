package org.setms.sew.core.inbound.tool;

import static org.setms.sew.core.inbound.tool.Inputs.acceptanceTests;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.sew.core.domain.model.sdlc.acceptance.ElementVariable;
import org.setms.sew.core.domain.model.sdlc.acceptance.Scenario;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.domain.model.validation.Diagnostic;

public class AcceptanceTestTool extends Tool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(acceptanceTests());
  }

  @Override
  public List<Output> getOutputs() {
    return List.of();
  }

  @Override
  protected void build(ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var acceptanceTests = inputs.get(AcceptanceTest.class);
    var reportSink = sink.select("reports/acceptanceTests");
    acceptanceTests.forEach(acceptanceTest -> build(acceptanceTest, reportSink, diagnostics));
  }

  private void build(
      AcceptanceTest acceptanceTest, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var report =
        sink.select(
            "%s-%s.html"
                .formatted(acceptanceTest.getSut().getId(), acceptanceTest.getSut().getType()));
    try (var writer = new PrintWriter(report.open())) {
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

  private String stateOf(Pointer state, AcceptanceTest acceptanceTest) {
    return format(state, acceptanceTest).map("contains %s"::formatted).orElse("is empty");
  }

  private Optional<String> format(Pointer variablePointer, AcceptanceTest acceptanceTest) {
    return Optional.ofNullable(variablePointer)
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
