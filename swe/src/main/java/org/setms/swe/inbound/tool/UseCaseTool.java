package org.setms.swe.inbound.tool;

import static java.util.Collections.emptyList;
import static org.setms.km.domain.model.format.Strings.*;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.artifact.UnresolvedArtifact;
import org.setms.km.domain.model.diagram.Arrow;
import org.setms.km.domain.model.diagram.BaseDiagramTool;
import org.setms.km.domain.model.diagram.Box;
import org.setms.km.domain.model.diagram.Diagram;
import org.setms.km.domain.model.diagram.IconBox;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.ddd.Domain;
import org.setms.swe.domain.model.sdlc.domainstory.DomainStory;
import org.setms.swe.domain.model.sdlc.domainstory.Sentence;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.eventstorming.Policy;
import org.setms.swe.domain.model.sdlc.eventstorming.ReadModel;
import org.setms.swe.domain.model.sdlc.stakeholders.User;
import org.setms.swe.domain.model.sdlc.usecase.Scenario;
import org.setms.swe.domain.model.sdlc.usecase.UseCase;
import org.setms.swe.domain.services.CreateAcceptanceTest;
import org.setms.swe.domain.services.DiscoverDomainFromUseCases;

public class UseCaseTool extends BaseDiagramTool {

  private static final Map<String, List<String>> ALLOWED_ATTRIBUTES =
      Map.of("event", List.of("updates"), "policy", List.of("reads"), "user", List.of("reads"));
  private static final Collection<String> DEPENDS_ON_ATTRIBUTES = List.of("reads");
  private static final String CREATE_MISSING_STEP = "step.missing.create";
  private static final String CREATE_DOMAIN = "domain.create";
  private static final String CREATE_ACCEPTANCE_TEST = "acceptance.test.create";
  private static final Collection<String> ACTIVE_ELEMENTS =
      List.of("aggregate", "policy", "readModel");
  private static final String CREATE_DOMAIN_STORY = "domainstory.create";

  @Override
  public Input<? extends Artifact> validationTarget() {
    return useCases();
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return Set.of(
        domainStories(),
        aggregates(),
        clockEvents(),
        commands(),
        events(),
        externalSystems(),
        policies(),
        readModels(),
        users(),
        entities(),
        domains(),
        acceptanceTests());
  }

  @Override
  public void validate(
      Artifact artifact, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var useCase = (UseCase) artifact;
    validateUseCase(useCase, inputs, diagnostics);
    if (diagnostics.isEmpty()) {
      validateAcceptanceTestsFor(useCase, inputs.get(AcceptanceTest.class), diagnostics);
    }
    validateDomainsFor(useCase, inputs.get(Domain.class), diagnostics);
  }

  private void validateUseCase(
      UseCase useCase, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var location = useCase.toLocation();
    useCase
        .getScenarios()
        .forEach(
            scenario ->
                validateScenario(scenario.appendTo(location), scenario, inputs, diagnostics));
  }

  private void validateScenario(
      Location location,
      Scenario scenario,
      ResolvedInputs inputs,
      Collection<Diagnostic> diagnostics) {
    validateDomainStory(location, scenario, inputs, diagnostics);
    var steps = scenario.getSteps();
    if (steps == null) {
      return;
    }
    validateStepReferences(location, steps, inputs, diagnostics);
  }

  private void validateDomainStory(
      Location location,
      Scenario scenario,
      ResolvedInputs inputs,
      Collection<Diagnostic> diagnostics) {
    if (scenario.getElaborates() == null) {
      diagnostics.add(new Diagnostic(WARN, "Scenario doesn't elaborate a domain story", location));
      return;
    }
    var resolved = inputs.resolve(scenario.getElaborates());
    if (resolved instanceof UnresolvedArtifact) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Unknown domain story",
              location,
              new Suggestion(CREATE_DOMAIN_STORY, "Create domain story")));
    }
  }

  private void validateStepReferences(
      Location location,
      List<Link> steps,
      ResolvedInputs inputs,
      Collection<Diagnostic> diagnostics) {
    steps.forEach(
        step -> {
          var stepLocation = location.plus("steps", steps, step);
          validateStepReference(step, inputs, diagnostics, stepLocation);
          step.getAttributes()
              .forEach(
                  (name, references) -> {
                    var allowed = ALLOWED_ATTRIBUTES.getOrDefault(step.getType(), emptyList());
                    if (allowed.contains(name)) {
                      references.forEach(
                          reference ->
                              validateStepReference(reference, inputs, diagnostics, stepLocation));
                    } else {
                      diagnostics.add(
                          new Diagnostic(
                              WARN, "Invalid attribute '%s'".formatted(name), stepLocation));
                    }
                  });
        });
  }

  private void validateStepReference(
      Link reference,
      ResolvedInputs inputs,
      Collection<Diagnostic> diagnostics,
      Location stepLocation) {
    if ("hotspot".equals(reference.getType())) {
      diagnostics.add(
          new Diagnostic(
              WARN, "Unresolved hotspot '%s'".formatted(reference.getId()), stepLocation));
    } else if (inputs.resolve(reference) instanceof UnresolvedArtifact) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Unknown %s '%s'".formatted(reference.getType(), reference.getId()),
              stepLocation,
              List.of(
                  new Suggestion(
                      CREATE_MISSING_STEP,
                      "Create %s '%s'".formatted(reference.getType(), reference.getId())))));
    }
  }

  private void validateAcceptanceTestsFor(
      UseCase useCase,
      Collection<AcceptanceTest> acceptanceTests,
      Collection<Diagnostic> diagnostics) {
    useCase.getScenarios().stream()
        .flatMap(Scenario::steps)
        .distinct()
        .filter(step -> ACTIVE_ELEMENTS.contains(step.getType()))
        .forEach(step -> validateAcceptanceTestFor(useCase, step, acceptanceTests, diagnostics));
  }

  private void validateAcceptanceTestFor(
      UseCase useCase,
      Link step,
      Collection<AcceptanceTest> acceptanceTests,
      Collection<Diagnostic> diagnostics) {
    if (acceptanceTests.stream().map(AcceptanceTest::getSut).noneMatch(step::equals)) {
      var scenario =
          useCase.scenarios().filter(s -> s.getSteps().contains(step)).findFirst().orElseThrow();
      var location = useCase.toLocation();
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing acceptance test for %s %s".formatted(step.getType(), step.getId()),
              scenario.appendTo(location).plus("steps", scenario.getSteps(), step),
              List.of(new Suggestion(CREATE_ACCEPTANCE_TEST, "Create acceptance test"))));
    }
  }

  private void validateDomainsFor(
      UseCase useCase, List<Domain> domains, Collection<Diagnostic> diagnostics) {
    if (domains.stream().noneMatch(d -> useCase.getPackage().equals(d.getPackage()))) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing subdomains",
              useCase.toLocation(),
              List.of(new Suggestion(CREATE_DOMAIN, "Discover subdomains"))));
    }
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> useCaseResource,
      Artifact artifact,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs)
      throws Exception {
    var useCase = (UseCase) artifact;
    return switch (suggestionCode) {
      case CREATE_DOMAIN_STORY -> createDomainStory(useCaseResource, useCase, location);
      case CREATE_MISSING_STEP -> createMissingStep(useCaseResource, useCase, inputs, location);
      case CREATE_DOMAIN -> createDomain(useCaseResource, useCase, inputs);
      case CREATE_ACCEPTANCE_TEST ->
          createAcceptanceTest(useCase, useCaseResource, location, inputs);
      case null, default -> unknown(suggestionCode);
    };
  }

  private AppliedSuggestion createDomainStory(
      Resource<?> useCaseResource, UseCase useCase, Location location) {
    return useCase
        .scenarios()
        .filter(scenario -> scenario.getName().equals(location.segments().get(4)))
        .map(Scenario::getElaborates)
        .filter(Objects::nonNull)
        .findFirst()
        .map(domainStoryLink -> createDomainStoryFor(useCaseResource, useCase, domainStoryLink))
        .orElseGet(() -> failedWith("Unknown scenario %s", location));
  }

  private AppliedSuggestion createDomainStoryFor(
      Resource<?> useCaseResource, UseCase useCase, Link domainStoryLink) {
    try {
      var packageName = useCase.getPackage();
      var domainStory =
          new DomainStory(new FullyQualifiedName(packageName, domainStoryLink.getId()))
              .setDescription("TODO: Add description and sentences.")
              .setSentences(List.of(dummySentence(packageName)));
      var domainStoryResource = resourceFor(domainStory, useCase, useCaseResource);
      try (var output = domainStoryResource.writeTo()) {
        builderFor(domainStory).build(domainStory, output);
      }
      return created(domainStoryResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  private org.setms.swe.domain.model.sdlc.domainstory.Sentence dummySentence(String packageName) {
    return new org.setms.swe.domain.model.sdlc.domainstory.Sentence(
            new FullyQualifiedName(packageName, "Sentence1"))
        .setParts(emptyList());
  }

  private AppliedSuggestion createMissingStep(
      Resource<?> useCaseResource, UseCase useCase, ResolvedInputs inputs, Location location) {
    return StepReference.find(location, inputs.get(UseCase.class))
        .map(
            stepReference ->
                createMissingStep(useCaseResource, useCase, stepReference.getStep().orElseThrow()))
        .orElseGet(() -> failedWith("Unknown step reference %s", location));
  }

  private AppliedSuggestion createMissingStep(
      Resource<?> useCaseResource, UseCase useCase, Link step) {
    var artifact = createMissingStep(useCase.getPackage(), step);
    var artifactResource = resourceFor(artifact, useCase, useCaseResource);
    try (var output = artifactResource.writeTo()) {
      builderFor(artifact).build(artifact, output);
    } catch (IOException e) {
      return failedWith(e);
    }
    return created(artifactResource);
  }

  private Artifact createMissingStep(String packageName, Link link) {
    var qualifiedName = new FullyQualifiedName(packageName, link.getId());
    var friendlyName = toFriendlyName(link.getId());
    return switch (link.getType()) {
      case "aggregate" -> new Aggregate(qualifiedName).setDisplay(friendlyName);
      case "command" -> new Command(qualifiedName).setDisplay(friendlyName);
      case "event" -> new Event(qualifiedName);
      case "policy" -> new Policy(qualifiedName).setTitle(friendlyName);
      case "readModel" -> new ReadModel(qualifiedName).setDisplay(friendlyName);
      case "user" -> new User(qualifiedName).setDisplay(friendlyName);
      default -> throw new UnsupportedOperationException("Unknown step reference " + link);
    };
  }

  private AppliedSuggestion createDomain(
      Resource<?> useCaseResource, UseCase useCase, ResolvedInputs inputs) throws IOException {
    var domain =
        new DiscoverDomainFromUseCases()
            .apply(
                inputs.get(UseCase.class).stream()
                    .filter(uc -> useCase.getPackage().equals(uc.getPackage()))
                    .toList());
    var domainResource = resourceFor(domain, useCase, useCaseResource);
    try (var output = domainResource.writeTo()) {
      builderFor(domain).build(domain, output);
    }
    return created(domainResource);
  }

  private AppliedSuggestion createAcceptanceTest(
      UseCase useCase, Resource<?> useCaseResource, Location location, ResolvedInputs inputs)
      throws IOException {
    var acceptanceTest = createAcceptanceTestFor(inputs, location);
    var acceptanceTestResource = resourceFor(acceptanceTest, useCase, useCaseResource);
    try (var output = acceptanceTestResource.writeTo()) {
      builderFor(acceptanceTest).build(acceptanceTest, output);
    }
    return created(acceptanceTestResource);
  }

  private AcceptanceTest createAcceptanceTestFor(ResolvedInputs inputs, Location location) {
    var useCases = inputs.get(UseCase.class);
    var step = StepReference.find(location, useCases).orElseThrow();
    return new CreateAcceptanceTest(step.getPackage(), inputs, useCases)
        .apply(step.getStep().orElseThrow());
  }

  @Override
  public Set<Input<? extends Artifact>> reportingContext() {
    return Set.of(domainStories(), useCases());
  }

  @Override
  public void buildReportsFor(
      Artifact useCase,
      ResolvedInputs inputs,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
    build((UseCase) useCase, inputs, resource.select(useCase.getName()), diagnostics);
  }

  private void build(
      UseCase useCase,
      ResolvedInputs inputs,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
    var report = resource.select(useCase.getName() + ".html");
    try (var writer = new PrintWriter(report.writeTo())) {
      writer.println("<html>");
      writer.println("  <body>");
      writer.printf("    <h1>%s</h1>%n", useCase.getTitle());
      if (isNotBlank(useCase.getDescription())) {
        writer.printf("    <p>%s</p>%n", useCase.getDescription());
      }
      useCase
          .scenarios()
          .forEach(
              scenario -> {
                var resolved = inputs.resolve(scenario.getElaborates());
                if (resolved instanceof DomainStory domainStory) {
                  writer.printf("    <h2>%s</h2>%n", resolved.friendlyName());
                  if (isNotBlank(domainStory.getDescription())) {
                    writer.printf("    <p>%s</p>%n", domainStory.getDescription());
                  }
                  writer.println("    <ol>");
                  domainStory
                      .sentences()
                      .map(Sentence::toHumanReadable)
                      .forEach(
                          sentence -> {
                            writer.print("      <li");
                            writer.printf(">%s", sentence);
                            writer.println("</li>");
                          });
                  writer.println("    </ol>");
                } else {
                  writer.printf("    <h2>%s</h2>%n", scenario.friendlyName());
                }
                if (scenario.getSteps() == null || scenario.getSteps().isEmpty()) {
                  return;
                }
                build(scenario, resource, diagnostics)
                    .ifPresent(
                        image ->
                            writer.printf(
                                "    <img src=\"%s\"/>%n",
                                report.toUri().resolve(".").normalize().relativize(image.toUri())));
              });
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private Optional<Resource<?>> build(
      Scenario scenario, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    return build(scenario, toDiagram(scenario), resource, diagnostics);
  }

  private Diagram toDiagram(Scenario scenario) {
    var result = new Diagram();
    var from = new AtomicReference<>(addBox(scenario.getSteps().getFirst(), result));
    scenario.getSteps().stream().skip(1).forEach(step -> addStepToGraph(step, from, result));

    return result;
  }

  private void addStepToGraph(Link step, AtomicReference<Box> lastBox, Diagram diagram) {
    var to = addBox(step, diagram);
    var previous = lastBox.get();
    diagram.add(new Arrow(previous, to));
    lastBox.set(to);
    step.getAttributes()
        .forEach(
            (name, references) -> {
              var begin = lastBox.get();
              for (var reference : references) {
                if (DEPENDS_ON_ATTRIBUTES.contains(name)) {
                  var arrows = diagram.findArrowsTo(previous);
                  var text = reference.getId();
                  var end =
                      arrows.stream()
                          .map(Arrow::to)
                          .filter(box -> text.equals(box.getText()))
                          .findFirst()
                          .orElseGet(() -> addBox(reference, diagram));
                  diagram.add(new Arrow(end, begin));
                } else {
                  var end = addBox(reference, diagram);
                  diagram.add(new Arrow(begin, end));
                }
              }
            });
  }

  private Box addBox(Link step, Diagram diagram) {
    return diagram.add(new IconBox(step.getId(), "eventStorm/" + step.getType()));
  }

  private record StepReference(UseCase useCase, String scenarioName, int stepIndex) {

    private static final Pattern PATTERN_STEP = Pattern.compile("steps\\[(?<index>\\d+)]");

    static Optional<StepReference> find(Location location, List<UseCase> useCases) {
      if (location.segments().size() < 5) {
        return Optional.empty();
      }
      var useCase = useCases.stream().filter(candidate -> candidate.starts(location)).findFirst();
      if (useCase.isEmpty()) {
        return Optional.empty();
      }
      var scenarioName = location.segments().get(4);
      var stepIndex = toStepIndex(location.segments().get(5));
      return Optional.of(new StepReference(useCase.get(), scenarioName, stepIndex));
    }

    private static int toStepIndex(String stepRef) {
      var matcher = PATTERN_STEP.matcher(stepRef);
      return matcher.matches() ? Integer.parseInt(matcher.group("index")) : -1;
    }

    Optional<Scenario> getScenario() {
      return useCase()
          .scenarios()
          .filter(scenario -> scenarioName().equals(scenario.getName()))
          .findFirst();
    }

    Optional<Link> getStep() {
      return getScenario().map(Scenario::getSteps).map(steps -> steps.get(stepIndex()));
    }

    String getPackage() {
      return useCase().getPackage();
    }
  }
}
