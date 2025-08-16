package org.setms.swe.inbound.tool;

import static java.util.Collections.emptyList;
import static org.setms.km.domain.model.format.Strings.*;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.*;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.artifact.UnresolvedArtifact;
import org.setms.km.domain.model.format.Strings;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.Tool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.ddd.Domain;
import org.setms.swe.domain.model.sdlc.domainstory.DomainStory;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.eventstorming.ExternalSystem;
import org.setms.swe.domain.model.sdlc.eventstorming.Policy;
import org.setms.swe.domain.model.sdlc.eventstorming.ReadModel;
import org.setms.swe.domain.model.sdlc.stakeholders.User;
import org.setms.swe.domain.model.sdlc.usecase.Scenario;
import org.setms.swe.domain.model.sdlc.usecase.UseCase;
import org.setms.swe.domain.services.CreateAcceptanceTest;
import org.setms.swe.domain.services.DiscoverDomainFromUseCases;
import org.setms.swe.inbound.format.acceptance.AcceptanceFormat;
import org.setms.swe.inbound.format.sal.SalFormat;

public class UseCaseTool extends Tool<UseCase> {

  private static final Map<String, List<String>> ALLOWED_ATTRIBUTES =
      Map.of("event", List.of("updates"), "policy", List.of("reads"), "user", List.of("reads"));
  private static final Collection<String> DEPENDS_ON_ATTRIBUTES = List.of("reads");
  private static final int ICON_SIZE = 52;
  private static final int MAX_TEXT_LENGTH = ICON_SIZE / 4;
  private static final String VERTEX_STYLE =
      "shape=image;image=%s;verticalLabelPosition=bottom;verticalAlign=top;fontColor=#6482B9;";
  private static final int LINE_HEIGHT = 16;
  private static final String CREATE_MISSING_STEP = "step.missing.create";
  private static final String CREATE_DOMAIN = "domain.create";
  private static final String CREATE_ACCEPTANCE_TEST = "acceptance.test.create";
  private static final Collection<String> ACTIVE_ELEMENTS =
      List.of("aggregate", "policy", "readModel");
  private static final String CREATE_DOMAIN_STORY = "domainstory.create";

  private JLanguageTool languageTool;

  @Override
  public Input<UseCase> getMainInput() {
    return useCases();
  }

  @Override
  public Set<Input<?>> additionalInputs() {
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
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var useCases = inputs.get(UseCase.class);
    useCases.forEach(useCase -> validateUseCase(useCase, inputs, diagnostics));
    if (diagnostics.isEmpty()) {
      validateAcceptanceTests(useCases, inputs, diagnostics);
    }
    if (!useCases.isEmpty()) {
      validateDomain(useCases, inputs, diagnostics);
    }
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

  private void validateAcceptanceTests(
      List<UseCase> useCases, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var acceptanceTests = inputs.get(AcceptanceTest.class);
    useCases.forEach(
        useCase ->
            useCase.getScenarios().stream()
                .flatMap(Scenario::steps)
                .distinct()
                .filter(step -> ACTIVE_ELEMENTS.contains(step.getType()))
                .forEach(
                    step ->
                        validateAcceptanceTestFor(useCase, step, acceptanceTests, diagnostics)));
  }

  private void validateAcceptanceTestFor(
      UseCase useCase,
      Link step,
      Collection<AcceptanceTest> acceptanceTests,
      Collection<Diagnostic> diagnostics) {
    if (acceptanceTests.stream().map(AcceptanceTest::getSut).noneMatch(step::equals)) {
      var scenario =
          useCase.scenarios().filter(s -> s.getSteps().contains(step)).findFirst().orElseThrow();
      Location location = useCase.toLocation();
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing acceptance test for %s %s".formatted(step.getType(), step.getId()),
              scenario.appendTo(location).plus("steps", scenario.getSteps(), step),
              List.of(new Suggestion(CREATE_ACCEPTANCE_TEST, "Create acceptance test"))));
    }
  }

  private void validateDomain(
      List<UseCase> useCases, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var domains = inputs.get(Domain.class);
    useCases.forEach(
        useCase -> {
          if (domains.stream().noneMatch(d -> useCase.getPackage().equals(d.getPackage()))) {
            diagnostics.add(
                new Diagnostic(
                    WARN,
                    "Missing subdomains",
                    useCase.toLocation(),
                    List.of(new Suggestion(CREATE_DOMAIN, "Discover subdomains"))));
          }
        });
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> useCaseResource,
      UseCase useCase,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs)
      throws Exception {
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
        new SalFormat().newBuilder().build(domainStory, output);
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
      new SalFormat().newBuilder().build(artifact, output);
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
      new SalFormat().newBuilder().build(domain, output);
    }
    return created(domainResource);
  }

  private AppliedSuggestion createAcceptanceTest(
      UseCase useCase, Resource<?> useCaseResource, Location location, ResolvedInputs inputs)
      throws IOException {
    var acceptanceTest = createAcceptanceTestFor(inputs, location);
    var acceptanceTestResource = resourceFor(acceptanceTest, useCase, useCaseResource);
    try (var output = acceptanceTestResource.writeTo()) {
      new AcceptanceFormat().newBuilder().build(acceptanceTest, output);
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
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var useCases = inputs.get(UseCase.class);
    useCases.forEach(
        useCase -> build(useCase, inputs, resource.select(useCase.getName()), diagnostics));
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
                writer.println("    <ol>");
                describeSteps(scenario.getSteps(), inputs)
                    .forEach(
                        sentence -> {
                          writer.print("      <li");
                          if (!sentence.valid()) {
                            writer.print(" style='color: #d32f2f;'");
                          }
                          writer.printf(">%s", sentence.text());
                          writer.println("</li>");
                        });
                writer.println("    </ol>");
              });
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private Optional<Resource<?>> build(
      Scenario scenario, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    return build(scenario, toGraph(scenario), resource, diagnostics);
  }

  private mxGraph toGraph(Scenario scenario) {
    var stepTexts = wrappedStepTextsFor(scenario);
    var numLines = ensureSameNumberOfLinesFor(stepTexts);
    var height = ICON_SIZE + (numLines - 1) * LINE_HEIGHT;
    var result = new mxGraph();
    result.getModel().beginUpdate();
    try {
      var from =
          new AtomicReference<>(
              addVertex(result, scenario.getSteps().getFirst(), height, stepTexts));
      scenario.getSteps().stream()
          .skip(1)
          .forEach(step -> addStepToGraph(step, result, height, stepTexts, from));
      layoutGraph(result, height);
    } finally {
      result.getModel().endUpdate();
    }

    return result;
  }

  private Map<Link, String> wrappedStepTextsFor(Scenario scenario) {
    var result = new HashMap<Link, String>();
    scenario
        .getSteps()
        .forEach(
            step -> {
              result.put(step, wrap(step.getId(), MAX_TEXT_LENGTH));
              step.getAttributes().values().stream()
                  .flatMap(Collection::stream)
                  .forEach(
                      reference -> result.put(reference, wrap(reference.getId(), MAX_TEXT_LENGTH)));
            });
    return result;
  }

  @SuppressWarnings("StringConcatenationInLoop")
  private int ensureSameNumberOfLinesFor(Map<Link, String> textsByStep) {
    var result = textsByStep.values().stream().mapToInt(this::numLinesIn).max().orElseThrow();
    textsByStep.forEach(
        (step, text) -> {
          var addLines = result - numLinesIn(text);
          for (var i = 0; i < addLines; i++) {
            text += NL;
          }
          textsByStep.put(step, text);
        });
    return result;
  }

  private int numLinesIn(String text) {
    return text.split(NL).length;
  }

  private void addStepToGraph(
      Link step,
      mxGraph graph,
      int vertexHeight,
      Map<Link, String> stepTexts,
      AtomicReference<Object> lastVertex) {
    var to = addVertex(graph, step, vertexHeight, stepTexts);
    var previous = lastVertex.get();
    graph.insertEdge(graph.getDefaultParent(), null, "", previous, to);
    lastVertex.set(to);
    step.getAttributes()
        .forEach(
            (name, references) -> {
              var begin = lastVertex.get();
              for (var reference : references) {
                if (DEPENDS_ON_ATTRIBUTES.contains(name)) {
                  var edges =
                      graph.getEdges(previous, graph.getDefaultParent(), false, true, false);
                  var text = stepTexts.get(reference);
                  var end =
                      Arrays.stream(edges)
                          .map(mxCell.class::cast)
                          .map(mxCell::getTarget)
                          .filter(vertex -> text.equals(vertex.getValue()))
                          .map(Object.class::cast)
                          .findFirst()
                          .orElseGet(() -> addVertex(graph, reference, vertexHeight, stepTexts));
                  graph.insertEdge(graph.getDefaultParent(), null, "", end, begin);
                } else {
                  var end = addVertex(graph, reference, vertexHeight, stepTexts);
                  graph.insertEdge(graph.getDefaultParent(), null, "", begin, end);
                }
              }
            });
  }

  private void layoutGraph(mxGraph graph, int height) {
    var layout = new mxHierarchicalLayout(graph, 7);
    layout.setInterRankCellSpacing(2.0 * ICON_SIZE / 3);
    layout.setIntraCellSpacing(height - ICON_SIZE + LINE_HEIGHT);
    layout.execute(graph.getDefaultParent());
  }

  private Object addVertex(
      mxGraph graph, Link step, int vertexHeight, Map<Link, String> stepTexts) {
    var url = getClass().getClassLoader().getResource("eventStorm/" + step.getType() + ".png");
    if (url == null) {
      throw new IllegalArgumentException("Icon not found for " + step.getType());
    }
    return graph.insertVertex(
        graph.getDefaultParent(),
        null,
        stepTexts.get(step),
        0,
        0,
        ICON_SIZE,
        vertexHeight,
        VERTEX_STYLE.formatted(url.toExternalForm()));
  }

  private List<Sentence> describeSteps(List<Link> steps, ResolvedInputs context) {
    var result = new ArrayList<Sentence>();
    var inputs = new ArrayList<Artifact>();
    Optional<Actor> actor = Optional.empty();
    var resolvedSteps = steps.stream().map(context::resolve).toList();
    for (var step : resolvedSteps) {
      if (actor.isPresent()) {
        var description = actor.get().getDescription(step);
        if (description.isPresent()) {
          var sentence = validate(description.get());
          result.add(sentence);
          inputs.clear();
          actor = Optional.empty();
        }
      } else {
        actor = Actor.from(step, false);
        if (actor.isEmpty()) {
          inputs.add(step);
        } else {
          actor.get().setInput(inputs);
        }
      }
    }
    actor.map(Actor::finishDescription).map(this::validate).ifPresent(result::add);
    if (inputs.size() == 1) {
      Actor.from(inputs.getFirst(), true)
          .map(Actor::finishDescription)
          .map(this::validate)
          .ifPresentOrElse(
              result::add, () -> System.err.printf("Unhandled final step %s%n", inputs.getFirst()));
    } else if (!inputs.isEmpty()) {
      System.err.printf("Additional unhandled steps: %s%n", inputs);
    }
    return result;
  }

  private Sentence validate(String text) {
    var sentence = text;
    var valid = true;
    var langTool = getLanguageTool();
    try {
      var matches = langTool.check(sentence);
      for (var match : matches) {
        if (match.getSuggestedReplacements().isEmpty()) {
          continue;
        }
        var replacement = match.getSuggestedReplacements().getFirst();
        if (replacement.equalsIgnoreCase(
            sentence.substring(match.getFromPos(), match.getToPos()))) {
          // Ignore changes in case only
          continue;
        }
        var improvement =
            validate(
                sentence.substring(0, match.getFromPos())
                    + replacement
                    + sentence.substring(match.getToPos()));
        if (improvement.valid()) {
          sentence = improvement.text();
        } else {
          valid = false;
        }
      }
    } catch (IOException e) {
      System.err.println("Error checking grammar: " + e.getMessage());
    }
    return new Sentence(sentence, valid);
  }

  private JLanguageTool getLanguageTool() {
    if (languageTool == null) {
      languageTool = new JLanguageTool(Languages.getLanguageForShortCode("en-US"));
    }
    return languageTool;
  }

  @Getter(AccessLevel.PROTECTED)
  @RequiredArgsConstructor
  private abstract static class Actor {

    private final String name;
    @Setter private List<Artifact> input;

    public static Optional<Actor> from(Artifact step, boolean atEnd) {
      if (isType(step, User.class)) {
        var userName = friendlyName(step, User.class, User::getDisplay);
        return Optional.of(new UserActor(userName));
      }
      if (isType(step, ExternalSystem.class)) {
        var externalSystemName =
            friendlyName(step, ExternalSystem.class, ExternalSystem::getDisplay);
        return Optional.of(new ExternalSystemActor(externalSystemName));
      }
      if (isType(step, Aggregate.class) || isType(step, Policy.class)) {
        return Optional.of(new SystemActor());
      }
      if (atEnd && isType(step, ReadModel.class)) {
        var readModelName = friendlyName(step, ReadModel.class, ReadModel::getDisplay);
        return Optional.of(new ReadModelActor(readModelName));
      }
      return Optional.empty();
    }

    protected static <T extends Artifact> boolean isType(Artifact object, Class<T> type) {
      return type.isInstance(object)
          || (object instanceof UnresolvedArtifact unresolvedObject
              && type.getSimpleName().equals(initUpper(unresolvedObject.type())));
    }

    protected static <T extends Artifact> String friendlyName(
        Artifact source, Class<T> type, Function<T, String> extractor) {
      var result = type.isInstance(source) ? extractor.apply(type.cast(source)) : null;
      return friendlyName(result == null ? source.getName() : result);
    }

    protected static String friendlyName(String name) {
      var result = new StringBuilder(name);
      for (var i = 1; i < result.length(); i++) {
        if (Character.isUpperCase(result.charAt(i))) {
          result.setCharAt(i, Character.toLowerCase(result.charAt(i)));
          result.insert(i, ' ');
        }
      }
      return result.toString();
    }

    public abstract Optional<String> getDescription(Artifact eventStormElement);

    public String finishDescription() {
      return null;
    }

    private static class UserActor extends Actor {

      public UserActor(String name) {
        super(name);
      }

      @Override
      public Optional<String> getDescription(Artifact eventStormElement) {
        if (isType(eventStormElement, Command.class)) {
          var command = friendlyName(eventStormElement, Command.class, Command::getDisplay);
          return Optional.of(userIssues(command));
        }
        getInput().add(eventStormElement);
        return Optional.empty();
      }

      private String userIssues(String command) {
        var user = initLower(getName());
        return "%s %s%s %s."
            .formatted(indefiniteArticleFor(user), user, describeInputs(), commandToText(command));
      }

      private String indefiniteArticleFor(String noun) {
        // Not 100%, but sentence validation will correct exceptions
        return Stream.of("a", "e", "i", "o").anyMatch(noun::startsWith) ? "An" : "A";
      }

      private String describeInputs() {
        if (getInput().isEmpty()) {
          return "";
        }
        var input = getInput().getFirst();
        if (isType(input, ReadModel.class)) {
          var readModelText = friendlyName(input, ReadModel.class, ReadModel::getDisplay);
          return ", looking at the %s,".formatted(initLower(readModelText));
        }
        if (isType(input, ExternalSystem.class)) {
          var externalSystemText =
              friendlyName(input, ExternalSystem.class, ExternalSystem::getDisplay);
          return ", via the %s,".formatted(initLower(externalSystemText));
        }
        return "";
      }
    }

    private static String commandToText(String command) {
      var words =
          Arrays.stream(command.split("\\s"))
              .map(Strings::initLower)
              .map(word -> word.equals("my") ? "their" : word)
              .collect(Collectors.toCollection(ArrayList::new));
      words.set(0, inflect(words.getFirst()));
      if (words.size() > 1 && !"the".equals(words.get(1)) && !"it".equals(words.get(1))) {
        words.add(1, "the");
      }
      return String.join(" ", words);
    }

    private static String inflect(String verb) {
      var result = verb;
      if (result.endsWith("y")) {
        result = result.substring(0, result.length() - 1) + "ies";
      } else if (result.endsWith("o") || result.endsWith("sh")) {
        result = result + "es";
      } else if (!result.endsWith("s")) {
        result = result + "s";
      }
      return result;
    }

    private static class SystemActor extends Actor {

      private final List<Artifact> actions = new ArrayList<>();

      public SystemActor() {
        super("The system");
      }

      @Override
      public Optional<String> getDescription(Artifact eventStormElement) {
        actions.add(eventStormElement);
        if (isType(eventStormElement, ReadModel.class)
            || isType(eventStormElement, Command.class)) {
          return Optional.of(describeActions());
        }
        return Optional.empty();
      }

      @Override
      public String finishDescription() {
        if (actions.isEmpty()) {
          return null;
        }
        var action = actions.getLast();
        if (isType(action, Event.class)) {
          var response =
              friendlyName(
                  action,
                  Event.class,
                  event -> Optional.ofNullable(event.getPayload()).map(Link::getId).orElse(""));
          return "%s responds that the %s.".formatted(getName(), initLower(response));
        }
        return actions.isEmpty() ? null : "%s does nothing.".formatted(getName());
      }

      private String describeActions() {
        var last = actions.getLast();
        if (isType(last, Command.class)) {
          var command = friendlyName(last, Command.class, Command::getDisplay);
          return "%s %s.".formatted(getName(), commandToText(command));
        }
        if (isType(last, ReadModel.class)) {
          var readModelText = friendlyName(last, ReadModel.class, ReadModel::getDisplay);
          return "%s updates the %s.".formatted(getName(), initLower(readModelText));
        }
        throw new IllegalStateException("Unknown last action: " + last.getClass().getSimpleName());
      }
    }

    private static class ExternalSystemActor extends Actor {

      public ExternalSystemActor(String externalSystemName) {
        super(externalSystemName);
      }

      @Override
      public Optional<String> getDescription(Artifact eventStormElement) {
        if (isType(eventStormElement, Event.class)) {
          var event = initLower(friendlyName(eventStormElement.getName()));
          return Optional.of(
              "The %s notifies the system that the %s."
                  .formatted(initLower(getName()), eventToText(event)));
        }
        getInput().add(eventStormElement);
        return Optional.empty();
      }

      private String eventToText(String event) {
        var words =
            Arrays.stream(event.split("\\s"))
                .map(Strings::initLower)
                .collect(Collectors.toCollection(ArrayList::new));
        words.add(words.size() - 1, "is");
        return String.join(" ", words);
      }
    }

    private static class ReadModelActor extends Actor {

      public ReadModelActor(String name) {
        super(name);
      }

      @Override
      public Optional<String> getDescription(Artifact eventStormElement) {
        return Optional.empty();
      }

      @Override
      public String finishDescription() {
        return "The system updates the %s.".formatted(initLower(getName()));
      }
    }
  }

  private record Sentence(String text, boolean valid) {}

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
