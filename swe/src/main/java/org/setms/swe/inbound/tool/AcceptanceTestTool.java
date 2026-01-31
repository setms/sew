package org.setms.swe.inbound.tool;

import static java.util.Collections.emptySet;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.swe.inbound.tool.Inputs.acceptanceTests;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.unitTests;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Level;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.AggregateScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ElementVariable;
import org.setms.swe.domain.model.sdlc.acceptancetest.PolicyScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ReadModelScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.Scenario;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;

@RequiredArgsConstructor
public class AcceptanceTestTool extends ArtifactTool<AcceptanceTest> {

  static final String SUGGESTION_CREATE_UNIT_TEST = "acceptance.test.create.unit.test";

  private final TechnologyResolver technologyResolver;

  public AcceptanceTestTool() {
    this(new TechnologyResolverImpl());
  }

  @Override
  public Input<AcceptanceTest> validationTarget() {
    return acceptanceTests();
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    var result = new HashSet<Input<? extends Artifact>>(unitTests());
    result.add(decisions());
    return result;
  }

  @Override
  protected Input<? extends Artifact> reportingTargetInput() {
    return acceptanceTests();
  }

  @Override
  public void validate(
      AcceptanceTest acceptanceTest, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    // TODO: Verify internal consistency, only proceed if OK
    var unitTest = find(inputs.get(UnitTest.class), acceptanceTest);
    if (unitTest.isEmpty()) {
      if (technologyResolver
          .unitTestGenerator(inputs.get(Decision.class), acceptanceTest.toLocation(), diagnostics)
          .isPresent()) {
        diagnostics.add(
            new Diagnostic(
                Level.WARN,
                "Missing unit test",
                acceptanceTest.toLocation(),
                new Suggestion(SUGGESTION_CREATE_UNIT_TEST, "Create unit test")));
      }
    } else {
      // TODO: Verify unit test implements all scenarios
    }
  }

  private Optional<UnitTest> find(List<UnitTest> unitTests, AcceptanceTest ignored) {
    // TODO: How to match against acceptanceTest?
    return unitTests.stream().findFirst();
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> resource,
      AcceptanceTest acceptanceTest,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs) {
    if (SUGGESTION_CREATE_UNIT_TEST.equals(suggestionCode)) {
      return createUnitTest(acceptanceTest, resource, inputs.get(Decision.class));
    }
    return technologyResolver.applySuggestion(suggestionCode, resource);
  }

  private AppliedSuggestion createUnitTest(
      AcceptanceTest acceptanceTest, Resource<?> resource, Collection<Decision> decisions) {
    var diagnostics = new HashSet<Diagnostic>();
    return technologyResolver
        .unitTestGenerator(decisions, acceptanceTest.toLocation(), diagnostics)
        .map(generator -> generator.generate(acceptanceTest))
        .map(unitTest -> store(unitTest, acceptanceTest, resource))
        .orElseGet(() -> new AppliedSuggestion(emptySet(), diagnostics));
  }

  private AppliedSuggestion store(
      UnitTest unitTest, AcceptanceTest acceptanceTest, Resource<?> acceptanceTestResource) {
    try {
      var unitTestResource = resourceFor(unitTest, acceptanceTest, acceptanceTestResource);
      try (var output = unitTestResource.writeTo()) {
        builderFor(unitTest).build(unitTest, output);
      }
      return created(unitTestResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  @Override
  public void buildReportsFor(
      AcceptanceTest acceptanceTest,
      ResolvedInputs inputs,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
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
    writer.printf("    <h2>%s</h2>%n", scenario.getName());
    switch (scenario) {
      case AggregateScenario aggregateScenario ->
          buildScenario(aggregateScenario, acceptanceTest, writer);
      case PolicyScenario policyScenario -> buildScenario(policyScenario, acceptanceTest, writer);
      case ReadModelScenario readModelScenario ->
          buildScenario(readModelScenario, acceptanceTest, writer);
      default ->
          throw new UnsupportedOperationException("Unknown acceptance test scenario: " + scenario);
    }
  }

  private void buildScenario(
      AggregateScenario scenario, AcceptanceTest acceptanceTest, PrintWriter writer) {
    var name = acceptanceTest.getSut().getId();
    writer.printf(
        "    <strong>Given</strong> <code>%s</code> %s<br/>%n",
        name, stateOf(scenario.getInit(), acceptanceTest));
    writer.printf(
        "    <strong>When</strong> <code>%s</code> accepts <code>%s</code><br/>%n",
        name, format(scenario.getAccepts(), acceptanceTest).orElse("?"));
    writer.printf(
        "    <strong>Then</strong> <code>%s</code> %s<br/>%n",
        name, stateOf(scenario.getState(), acceptanceTest));
    writer.printf(
        "    <strong>And</strong> <code>%s</code> emits <code>%s</code><br/>%n",
        name, format(scenario.getEmitted(), acceptanceTest).orElse("?"));
  }

  private String stateOf(List<Link> state, AcceptanceTest acceptanceTest) {
    var items =
        Optional.ofNullable(state).stream()
            .flatMap(Collection::stream)
            .map(item -> format(item, acceptanceTest))
            .flatMap(Optional::stream)
            .toList();
    return switch (items.size()) {
      case 0 -> "is empty";
      case 1 -> "contains " + items.getFirst();
      case 2 -> "contains %s and %s".formatted(items.getFirst(), items.getLast());
      default ->
          "contains %s, and %s"
              .formatted(String.join(", ", items.subList(0, items.size() - 1)), items.getLast());
    };
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

  private void buildScenario(
      PolicyScenario scenario, AcceptanceTest acceptanceTest, PrintWriter writer) {
    var name = acceptanceTest.getSut().getId();
    writer.printf(
        "    <strong>Given</strong> <code>%s</code> %s<br/>%n",
        name, stateOf(scenario.getInit(), acceptanceTest));
    writer.printf(
        "    <strong>When</strong> <code>%s</code> handles <code>%s</code><br/>%n",
        name, format(scenario.getHandles(), acceptanceTest).orElse("?"));
    writer.printf(
        "    <strong>Then</strong> <code>%s</code> issues <code>%s</code><br/>%n",
        name, format(scenario.getIssued(), acceptanceTest).orElse("?"));
  }

  private void buildScenario(
      ReadModelScenario scenario, AcceptanceTest acceptanceTest, PrintWriter writer) {
    var name = acceptanceTest.getSut().getId();
    writer.printf(
        "    <strong>Given</strong> <code>%s</code> %s<br/>%n",
        name, stateOf(scenario.getInit(), acceptanceTest));
    writer.printf(
        "    <strong>When</strong> <code>%s</code> handles <code>%s</code><br/>%n",
        name, format(scenario.getHandles(), acceptanceTest).orElse("?"));
    writer.printf(
        "    <strong>Then</strong> <code>%s</code> <code>%s</code><br/>%n",
        name, stateOf(scenario.getState(), acceptanceTest));
  }
}
