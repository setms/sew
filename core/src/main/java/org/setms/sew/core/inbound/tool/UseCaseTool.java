package org.setms.sew.core.inbound.tool;

import static java.util.Collections.emptyList;
import static org.setms.sew.core.domain.model.format.Strings.initLower;
import static org.setms.sew.core.domain.model.format.Strings.initUpper;
import static org.setms.sew.core.domain.model.format.Strings.isNotBlank;
import static org.setms.sew.core.domain.model.tool.Level.ERROR;
import static org.setms.sew.core.domain.model.tool.Level.WARN;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.atteo.evo.inflector.English;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.setms.sew.core.domain.model.format.Strings;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.sew.core.domain.model.sdlc.ddd.Domain;
import org.setms.sew.core.domain.model.sdlc.design.Entity;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.sew.core.domain.model.sdlc.eventstorming.ClockEvent;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Command;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Event;
import org.setms.sew.core.domain.model.sdlc.eventstorming.ExternalSystem;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Policy;
import org.setms.sew.core.domain.model.sdlc.eventstorming.ReadModel;
import org.setms.sew.core.domain.model.sdlc.stakeholders.User;
import org.setms.sew.core.domain.model.sdlc.usecase.Scenario;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Location;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Suggestion;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.domain.model.tool.UnresolvedObject;
import org.setms.sew.core.domain.services.CreateAcceptanceTest;
import org.setms.sew.core.domain.services.DiscoverDomainFromUseCases;
import org.setms.sew.core.inbound.format.acceptance.AcceptanceFormat;
import org.setms.sew.core.inbound.format.sew.SewFormat;

public class UseCaseTool extends Tool {

  private static final String OUTPUT_PATH = "build/reports/useCases";
  private static final List<String> ELEMENT_ORDER =
      List.of(
          "readModel",
          "user",
          "command",
          "aggregate",
          "event",
          "policy",
          "externalSystem",
          "hotspot");
  private static final Map<String, Collection<String>> ALLOWED_FOLLOWING =
      Map.of(
          "readModel",
          List.of("user", "policy", "event", "hotspot"),
          "user",
          List.of("command", "policy", "externalSystem", "hotspot"),
          "externalSystem",
          List.of("command", "event", "hotspot"),
          "command",
          List.of("aggregate", "externalSystem", "hotspot"),
          "aggregate",
          List.of("event", "hotspot"),
          "event",
          List.of("policy", "externalSystem", "readModel", "hotspot"),
          "clockEvent",
          List.of("policy", "externalSystem", "readModel", "hotspot"),
          "calendarEvent",
          List.of("policy", "externalSystem", "readModel", "hotspot"),
          "policy",
          List.of("command", "hotspot"),
          "hotspot",
          ELEMENT_ORDER);
  private static final Collection<String> ALLOWED_ENDING =
      List.of("event", "hotspot", "policy", "readModel", "externalSystem", "user");
  private static final Map<String, String> VERBS =
      Map.of("event", "emit", "command", "issue", "readModel", "update");
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
  private JLanguageTool languageTool;

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>("src/main/requirements", UseCase.class),
        new Input<>("src/main/design", Aggregate.class),
        new Input<>("src/main/design", ClockEvent.class),
        new Input<>("src/main/design", Command.class),
        new Input<>("src/main/design", Event.class),
        new Input<>("src/main/design", ExternalSystem.class),
        new Input<>("src/main/design", Policy.class),
        new Input<>("src/main/design", ReadModel.class),
        new Input<>("src/main/stakeholders", User.class),
        new Input<>("src/main/design", Entity.class),
        new Input<>("src/main/requirements", Domain.class),
        new Input<>(
            "src/test/acceptance", new AcceptanceFormat(), AcceptanceTest.class, "acceptance"));
  }

  @Override
  public List<Output> getOutputs() {
    return htmlWithImages(OUTPUT_PATH);
  }

  @Override
  protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
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
    var location = new Location(useCase);
    useCase
        .getScenarios()
        .forEach(
            scenario ->
                validateScenario(
                    location.plus(scenario), scenario.getSteps(), inputs, diagnostics));
  }

  private void validateScenario(
      Location location,
      List<Pointer> steps,
      ResolvedInputs inputs,
      Collection<Diagnostic> diagnostics) {
    if (steps == null) {
      return;
    }
    validateStepReferences(location, steps, inputs, diagnostics);
    validateGrammar(location, steps, diagnostics);
  }

  private void validateStepReferences(
      Location location,
      List<Pointer> steps,
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
      Pointer reference,
      ResolvedInputs inputs,
      Collection<Diagnostic> diagnostics,
      Location stepLocation) {
    var types = English.plural(reference.getType());
    var candidates = inputs.get(types, NamedObject.class);
    if ("hotspot".equals(reference.getType())) {
      diagnostics.add(
          new Diagnostic(
              WARN, "Unresolved hotspot '%s'".formatted(reference.getId()), stepLocation));
    } else if (reference.resolveFrom(candidates).isEmpty()) {
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

  private void validateGrammar(
      Location location, List<Pointer> steps, Collection<Diagnostic> diagnostics) {
    if (steps.isEmpty()) {
      return;
    }
    var prev = new AtomicReference<>(steps.getFirst());
    steps.stream()
        .skip(1)
        .forEach(
            step -> {
              var previous = prev.get();
              var allowed = ALLOWED_FOLLOWING.getOrDefault(previous.getType(), emptyList());
              if (!allowed.contains(step.getType())) {
                diagnostics.add(
                    new Diagnostic(
                        ERROR,
                        "%s can't %s %s"
                            .formatted(
                                initUpper(English.plural(previous.getType())),
                                VERBS.getOrDefault(step.getType(), "precede"),
                                English.plural(step.getType())),
                        location.plus("steps", steps, step)));
              }
              prev.set(step);
            });
    var last = steps.getLast();
    if (!ALLOWED_ENDING.contains(last.getType())) {
      diagnostics.add(
          new Diagnostic(
              ERROR,
              "Can't end with %s".formatted(last.getType()),
              location.plus(last.getType(), last.getId())));
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
      Pointer step,
      Collection<AcceptanceTest> acceptanceTests,
      Collection<Diagnostic> diagnostics) {
    if (acceptanceTests.stream().map(AcceptanceTest::getSut).noneMatch(step::equals)) {
      var scenario =
          useCase.scenarios().filter(s -> s.getSteps().contains(step)).findFirst().orElseThrow();
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing acceptance test for %s %s".formatted(step.getType(), step.getId()),
              new Location(useCase).plus(scenario).plus("steps", scenario.getSteps(), step),
              List.of(new Suggestion(CREATE_ACCEPTANCE_TEST, "Create acceptance test"))));
    }
  }

  private void validateDomain(
      List<UseCase> useCases, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var domains = inputs.get(Domain.class);
    useCases.stream()
        .map(UseCase::getPackage)
        .distinct()
        .forEach(
            packageName -> {
              if (domains.stream().noneMatch(d -> packageName.equals(d.getPackage()))) {
                diagnostics.add(
                    new Diagnostic(
                        WARN,
                        "Missing subdomains",
                        new Location(packageName),
                        List.of(new Suggestion(CREATE_DOMAIN, "Discover subdomains"))));
              }
            });
  }

  @Override
  protected void apply(
      String suggestionCode,
      ResolvedInputs inputs,
      Location location,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    switch (suggestionCode) {
      case CREATE_MISSING_STEP -> createMissingStep(inputs, location, sink, diagnostics);
      case CREATE_DOMAIN -> createDomain(location.segments().getLast(), inputs, sink, diagnostics);
      case CREATE_ACCEPTANCE_TEST -> createAcceptanceTest(inputs, location, sink, diagnostics);
      case null, default -> super.apply(suggestionCode, inputs, location, sink, diagnostics);
    }
  }

  private void createMissingStep(
      ResolvedInputs inputs,
      Location location,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    StepReference.find(location, inputs.get(UseCase.class))
        .ifPresentOrElse(
            stepReference ->
                createMissingStep(
                    stepReference.useCase().getPackage(),
                    stepReference.getStep().orElseThrow(),
                    normalize(sink),
                    diagnostics),
            () -> addError(diagnostics, "Unknown step reference %s", location));
  }

  private OutputSink normalize(OutputSink sink) {
    if (sink.toUri().toString().endsWith(".useCase")) {
      return sink.select("../../../..");
    }
    return sink;
  }

  private void createMissingStep(
      String packageName, Pointer step, OutputSink output, Collection<Diagnostic> diagnostics) {
    var type = step.getType();
    var sink = output.select("src/main");
    if ("user".equals(type)) {
      sink = sink.select("stakeholders");
    } else {
      sink = sink.select("design");
    }
    var name = step.getId();
    sink = sink.select("%s.%s".formatted(name, type));
    try (var writer = new PrintWriter(sink.open())) {
      writer.printf("package %s%n%n", packageName);
      writer.printf("%s %s {%n", type, name);
      writeProperties(type, name, writer);
      writer.println("}");
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
    diagnostics.add(sinkCreated(sink));
  }

  private void writeProperties(String type, String name, PrintWriter writer) {
    var nameProperty =
        switch (type) {
          case "aggregate", "command", "readModel", "user" -> "display";
          case "policy" -> "title";
          default -> null;
        };
    if (nameProperty == null) {
      return;
    }
    writer.printf("  %s = \"%s\"%n", nameProperty, toFriendlyName(name));
  }

  private String toFriendlyName(String name) {
    var result = new StringBuilder(name);
    for (var i = 1; i < result.length(); i++) {
      if (Character.isUpperCase(result.charAt(i))) {
        result.setCharAt(i, Character.toLowerCase(result.charAt(i)));
        result.insert(i, ' ');
      }
    }
    return result.toString();
  }

  private void createDomain(
      String packageName,
      ResolvedInputs inputs,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    try {
      var domain =
          new DiscoverDomainFromUseCases()
              .apply(
                  inputs.get(UseCase.class).stream()
                      .filter(uc -> packageName.equals(uc.getPackage()))
                      .toList());
      var domainSink =
          normalize(sink).select("src/main/requirements/%s.domain".formatted(domain.getName()));
      try (var output = domainSink.open()) {
        new SewFormat().newBuilder().build(domain, output);
      }
      diagnostics.add(sinkCreated(domainSink));
    } catch (Exception e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private void createAcceptanceTest(
      ResolvedInputs inputs,
      Location location,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    try {
      var acceptanceTest = createAcceptanceTestFor(inputs, location);
      var acceptanceTestSink =
          normalize(sink)
              .select(
                  "src/test/acceptance/%s-%s.acceptance"
                      .formatted(acceptanceTest.getName(), acceptanceTest.getSut().getType()));
      try (var output = acceptanceTestSink.open()) {
        new AcceptanceFormat().newBuilder().build(acceptanceTest, output);
      }
      diagnostics.add(sinkCreated(acceptanceTestSink));
    } catch (Exception e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private AcceptanceTest createAcceptanceTestFor(ResolvedInputs inputs, Location location) {
    var useCases = inputs.get(UseCase.class);
    var step = StepReference.find(location, useCases).orElseThrow();
    return new CreateAcceptanceTest(step.getPackage(), inputs, useCases)
        .apply(step.getStep().orElseThrow());
  }

  @Override
  public void build(ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var useCases = inputs.get(UseCase.class);
    var reportSink = sink.select("reports/useCases");
    useCases.forEach(useCase -> build(useCase, inputs, reportSink, diagnostics));
  }

  private void build(
      UseCase useCase, ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var report = sink.select(useCase.getName() + ".html");
    try (var writer = new PrintWriter(report.open())) {
      writer.println("<html>");
      writer.println("  <body>");
      writer.printf("    <h1>%s</h1>%n", useCase.getTitle());
      if (isNotBlank(useCase.getDescription())) {
        writer.printf("    <p>%s</p>%n", useCase.getDescription());
      }
      useCase
          .getScenarios()
          .forEach(
              scenario -> {
                writer.printf("    <h2>%s</h2>%n", scenario.getTitle());
                if (isNotBlank(scenario.getDescription())) {
                  writer.printf("    <p>%s</p>%n", scenario.getDescription());
                }
                if (scenario.getSteps() == null || scenario.getSteps().isEmpty()) {
                  return;
                }
                var image = build(scenario, sink, diagnostics);
                writer.printf(
                    "    <img src=\"%s\"/>%n",
                    report.toUri().resolve(".").normalize().relativize(image.toUri()));
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

  private OutputSink build(Scenario scenario, OutputSink sink, Collection<Diagnostic> diagnostics) {
    return build(scenario, toGraph(scenario), sink, diagnostics);
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

  private Map<Pointer, String> wrappedStepTextsFor(Scenario scenario) {
    var result = new HashMap<Pointer, String>();
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
  private int ensureSameNumberOfLinesFor(Map<Pointer, String> textsByPointer) {
    var result = textsByPointer.values().stream().mapToInt(this::numLinesIn).max().orElseThrow();
    textsByPointer.forEach(
        (pointer, text) -> {
          var addLines = result - numLinesIn(text);
          for (var i = 0; i < addLines; i++) {
            text += NL;
          }
          textsByPointer.put(pointer, text);
        });
    return result;
  }

  private int numLinesIn(String text) {
    return text.split(NL).length;
  }

  private void addStepToGraph(
      Pointer step,
      mxGraph graph,
      int vertexHeight,
      Map<Pointer, String> stepTexts,
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
      mxGraph graph, Pointer step, int vertexHeight, Map<Pointer, String> stepTexts) {
    var url = getClass().getClassLoader().getResource("resin/" + step.getType() + ".png");
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

  private List<Sentence> describeSteps(List<Pointer> steps, ResolvedInputs context) {
    var result = new ArrayList<Sentence>();
    var inputs = new ArrayList<NamedObject>();
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
    @Setter private List<NamedObject> input;

    public static Optional<Actor> from(NamedObject step, boolean atEnd) {
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

    protected static <T extends NamedObject> boolean isType(NamedObject object, Class<T> type) {
      return type.isInstance(object)
          || (object instanceof UnresolvedObject unresolvedObject
              && type.getSimpleName().equals(initUpper(unresolvedObject.type())));
    }

    protected static <T extends NamedObject> String friendlyName(
        NamedObject source, Class<T> type, Function<T, String> extractor) {
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

    public abstract Optional<String> getDescription(NamedObject eventStormElement);

    public String finishDescription() {
      return null;
    }

    private static class UserActor extends Actor {

      public UserActor(String name) {
        super(name);
      }

      @Override
      public Optional<String> getDescription(NamedObject eventStormElement) {
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

      private final List<NamedObject> actions = new ArrayList<>();

      public SystemActor() {
        super("The system");
      }

      @Override
      public Optional<String> getDescription(NamedObject eventStormElement) {
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
                  event -> Optional.ofNullable(event.getPayload()).map(Pointer::getId).orElse(""));
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
      public Optional<String> getDescription(NamedObject eventStormElement) {
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
      public Optional<String> getDescription(NamedObject eventStormElement) {
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
      var useCase = useCases.stream().filter(location::isInside).findFirst();
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

    Optional<Pointer> getStep() {
      return useCase()
          .scenarios()
          .filter(scenario -> scenarioName().equals(scenario.getName()))
          .map(Scenario::getSteps)
          .map(steps -> steps.get(stepIndex()))
          .findAny();
    }

    String getPackage() {
      return useCase().getPackage();
    }
  }
}
