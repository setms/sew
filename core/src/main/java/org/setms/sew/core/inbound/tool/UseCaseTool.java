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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.atteo.evo.inflector.English;
import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.setms.sew.core.domain.model.sdlc.Aggregate;
import org.setms.sew.core.domain.model.sdlc.ClockEvent;
import org.setms.sew.core.domain.model.sdlc.Command;
import org.setms.sew.core.domain.model.sdlc.Domains;
import org.setms.sew.core.domain.model.sdlc.Event;
import org.setms.sew.core.domain.model.sdlc.ExternalSystem;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.Policy;
import org.setms.sew.core.domain.model.sdlc.ReadModel;
import org.setms.sew.core.domain.model.sdlc.UseCase;
import org.setms.sew.core.domain.model.sdlc.User;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.InputSource;
import org.setms.sew.core.domain.model.tool.Location;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Suggestion;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.domain.model.tool.UnresolvedObject;
import org.setms.sew.core.domain.services.GenerateDomainsFromUseCases;
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
      List.of("event", "hotspot", "policy", "readModel", "externalSystem");
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
  private static final String CREATE_DOMAINS = "domains.create";
  private static final Pattern PATTERN_STEP = Pattern.compile("steps\\[(?<index>\\d+)]");

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>(
            "useCases",
            new Glob("src/main/requirements", "**/*.useCase"),
            new SewFormat(),
            UseCase.class),
        new Input<>(
            "aggregates",
            new Glob("src/main/design", "**/*.aggregate"),
            new SewFormat(),
            Aggregate.class),
        new Input<>(
            "clockEvents",
            new Glob("src/main/design", "**/*.clockEvent"),
            new SewFormat(),
            ClockEvent.class),
        new Input<>(
            "commands",
            new Glob("src/main/design", "**/*.command"),
            new SewFormat(),
            Command.class),
        new Input<>(
            "events", new Glob("src/main/design", "**/*.event"), new SewFormat(), Event.class),
        new Input<>(
            "externalSystems",
            new Glob("src/main/design", "**/*.externalSystem"),
            new SewFormat(),
            ExternalSystem.class),
        new Input<>(
            "policies", new Glob("src/main/design", "**/*.policy"), new SewFormat(), Policy.class),
        new Input<>(
            "readModels",
            new Glob("src/main/design", "**/*.readModel"),
            new SewFormat(),
            ReadModel.class),
        new Input<>(
            "users", new Glob("src/main/stakeholders", "**/*.user"), new SewFormat(), User.class),
        new Input<>(
            "domains",
            new Glob("src/main/architecture", "**/*.domains"),
            new SewFormat(),
            Domains.class));
  }

  @Override
  public List<Output> getOutputs() {
    return List.of(
        new Output(new Glob(OUTPUT_PATH, "*.html")), new Output(new Glob(OUTPUT_PATH, "*.png")));
  }

  @Override
  protected void validate(
      InputSource source, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var useCases = inputs.get("useCases", UseCase.class);
    useCases.forEach(
        useCase ->
            useCase
                .getScenarios()
                .forEach(
                    scenario ->
                        validateScenario(
                            new Location(
                                "useCase", useCase.getName(), "scenario", scenario.getName()),
                            scenario.getSteps(),
                            inputs,
                            diagnostics)));
    if (!useCases.isEmpty()) {
      var domains = inputs.get("domains", Domains.class);
      if (domains.isEmpty()) {
        diagnostics.add(
            new Diagnostic(
                WARN,
                "Missing domains",
                null,
                List.of(new Suggestion(CREATE_DOMAINS, "Group into domains"))));
      }
    }
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
          var stepLocation = location.plus("steps[%d]".formatted(steps.indexOf(step)));
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
    if (reference.resolveFrom(candidates).isEmpty()) {
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
                        location.plus("steps[%d]".formatted(steps.indexOf(step)))));
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

  @Override
  protected void apply(
      String suggestionCode,
      ResolvedInputs inputs,
      Location location,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    if (CREATE_MISSING_STEP.equals(suggestionCode)) {
      createMissingStep(inputs, location, sink, diagnostics);
    } else if (CREATE_DOMAINS.equals(suggestionCode)) {
      createDomains(inputs, sink, diagnostics);
    } else {
      super.apply(suggestionCode, inputs, location, sink, diagnostics);
    }
  }

  private void createMissingStep(
      ResolvedInputs inputs,
      Location location,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    var useCaseName = location.segments().get(1);
    var scenarioName = location.segments().get(3);
    var stepRef = location.segments().get(4);
    var matcher = PATTERN_STEP.matcher(stepRef);
    if (matcher.matches()) {
      var stepIndex = Integer.parseInt(matcher.group("index"));
      createMissingStep(inputs, useCaseName, scenarioName, stepIndex, sink, diagnostics);
    } else {
      addError(
          diagnostics,
          "Unknown step reference %s in scenario %s of use case %s",
          stepRef,
          scenarioName,
          useCaseName);
    }
  }

  private void createMissingStep(
      ResolvedInputs inputs,
      String useCaseName,
      String scenarioName,
      int stepIndex,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    inputs.get("useCases", UseCase.class).stream()
        .filter(useCase -> useCaseName.equals(useCase.getName()))
        .findFirst()
        .ifPresentOrElse(
            useCase -> createMissingStep(useCase, scenarioName, stepIndex, sink, diagnostics),
            () -> addError(diagnostics, "Unknown use case %s", useCaseName));
  }

  private void createMissingStep(
      UseCase useCase,
      String scenarioName,
      int stepIndex,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    useCase.getScenarios().stream()
        .filter(scenario -> scenarioName.equals(scenario.getName()))
        .findFirst()
        .ifPresentOrElse(
            scenario ->
                createMissingStep(useCase, scenario, stepIndex, normalize(sink), diagnostics),
            () ->
                addError(
                    diagnostics,
                    "Unknown scenario %s in use case %s",
                    scenarioName,
                    useCase.getName()));
  }

  private OutputSink normalize(OutputSink sink) {
    if (sink.toUri().toString().endsWith(".useCase")) {
      return sink.select("../../../..");
    }
    return sink;
  }

  private void createMissingStep(
      UseCase useCase,
      UseCase.Scenario scenario,
      int stepIndex,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    if (0 <= stepIndex && stepIndex < scenario.getSteps().size()) {
      createMissingStep(
          useCase.getPackage(), scenario.getSteps().get(stepIndex), sink, diagnostics);
    } else {
      addError(
          diagnostics,
          "Invalid step %d for scenario %s in use case %s",
          stepIndex + 1,
          scenario.getName(),
          useCase.getName());
    }
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

  private void createDomains(
      ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    try {
      var domains = new GenerateDomainsFromUseCases().apply(inputs.get("useCases", UseCase.class));
      var domainsSink =
          normalize(sink).select("src/main/architecture/%s.domains".formatted(domains.getName()));
      try (var output = domainsSink.open()) {
        new SewFormat().newBuilder().build(domains, output);
      }
      diagnostics.add(sinkCreated(domainsSink));
    } catch (Exception e) {
      addError(diagnostics, e.getMessage());
    }
  }

  @Override
  public void build(ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var useCases = inputs.get("useCases", UseCase.class);
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
                writer.print("      <li>");
                writer.print(
                    String.join(
                        "</li>%s      <li>".formatted(System.lineSeparator()),
                        describeSteps(scenario.getSteps(), inputs)));
                writer.println("</li>");
                writer.println("    </ol>");
              });
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private OutputSink build(
      UseCase.Scenario scenario, OutputSink sink, Collection<Diagnostic> diagnostics) {
    return build(scenario, toGraph(scenario), sink, diagnostics);
  }

  private mxGraph toGraph(UseCase.Scenario scenario) {
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

  private Map<Pointer, String> wrappedStepTextsFor(UseCase.Scenario scenario) {
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

  private List<String> describeSteps(List<Pointer> steps, ResolvedInputs context) {
    var result = new ArrayList<String>();
    var inputs = new ArrayList<NamedObject>();
    Optional<Actor> actor = Optional.empty();
    var resolvedSteps = steps.stream().map(context::resolve).toList();
    for (var step : resolvedSteps) {
      if (actor.isPresent()) {
        var description = actor.get().getDescription(step);
        if (description.isPresent()) {
          var sentence = description.get();
          validate(sentence);
          result.add(sentence);
          inputs.clear();
          actor = Optional.empty();
        }
      } else {
        actor = Actor.from(step);
        if (actor.isEmpty()) {
          inputs.add(step);
        } else {
          actor.get().setInput(inputs);
        }
      }
    }
    actor.map(Actor::finishDescription).ifPresent(result::add);
    return result;
  }

  private void validate(String sentence) {
    var langTool = new JLanguageTool(Languages.getLanguageForShortCode("en"));
    try {
      var matches = langTool.check(sentence);
      for (var match : matches) {
        System.err.printf(
            "Potential error in sentence '%s' at characters %d-%d: %s%n",
            sentence, match.getFromPos(), match.getToPos(), match.getMessage());
        System.err.println("- Suggested correction(s): " + match.getSuggestedReplacements());
      }
    } catch (IOException e) {
      System.err.println("Error checking grammar: " + e.getMessage());
    }
  }

  @Getter(AccessLevel.PROTECTED)
  @RequiredArgsConstructor
  private abstract static class Actor {

    private final String name;
    @Setter private List<NamedObject> input;

    public static Optional<Actor> from(NamedObject step) {
      if (isType(step, User.class)) {
        var userName = friendlyName(step, User.class, User::getDisplay);
        return Optional.of(new UserActor(userName));
      }
      if (isType(step, Aggregate.class) || isType(step, Policy.class)) {
        return Optional.of(new SystemActor());
      }
      return Optional.empty();
    }

    protected static <T extends NamedObject> boolean isType(NamedObject object, Class<T> type) {
      return type.isInstance(object)
          || (object instanceof UnresolvedObject unresolvedObject
              && type.getSimpleName().equals(initUpper(unresolvedObject.getType())));
    }

    protected static <T extends NamedObject> String friendlyName(
        NamedObject source, Class<T> type, Function<T, String> extractor) {
      var result = type.isInstance(source) ? extractor.apply(type.cast(source)) : null;
      return friendlyName(result == null ? source.getName() : result);
    }

    private static String friendlyName(String name) {
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
        return "The %s%s %s."
            .formatted(initLower(getName()), describeInputs(), commandToText(command));
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
      var result = initLower(command);
      var index = result.indexOf(' ');
      if (index > 0 && result.charAt(index - 1) != 's') {
        result = result.substring(0, index) + "s" + result.substring(index);
      }
      index = result.indexOf(" my ");
      if (index > 0) {
        result = result.substring(0, index) + " their " + result.substring(index + 4);
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
          var response = friendlyName(action, Event.class, event -> event.getPayload().getId());
          return "%s responds with the %s.".formatted(getName(), initLower(response));
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
  }
}
