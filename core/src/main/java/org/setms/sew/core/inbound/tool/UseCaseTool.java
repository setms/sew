package org.setms.sew.core.inbound.tool;

import static java.util.Collections.emptyList;
import static org.setms.sew.core.domain.model.format.Strings.initLower;
import static org.setms.sew.core.domain.model.format.Strings.initUpper;
import static org.setms.sew.core.domain.model.format.Strings.isNotBlank;
import static org.setms.sew.core.domain.model.tool.Level.ERROR;
import static org.setms.sew.core.domain.model.tool.Level.WARN;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import javax.imageio.ImageIO;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.atteo.evo.inflector.English;
import org.setms.sew.core.domain.model.sdlc.Aggregate;
import org.setms.sew.core.domain.model.sdlc.Command;
import org.setms.sew.core.domain.model.sdlc.Event;
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
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.domain.model.tool.UnresolvedObject;
import org.setms.sew.core.inbound.format.sew.SewFormat;

public class UseCaseTool extends Tool {

  private static final String OUTPUT_PATH = "build/reports/useCases";
  private static final int ICON_SIZE = 80;
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
      List.of("event", "hotspot", "readModel", "externalSystem");
  private static final Map<String, String> VERBS =
      Map.of("event", "emit", "command", "issue", "readModel", "update");
  private static final Map<String, List<String>> ALLOWED_ATTRIBUTES =
      Map.of("event", List.of("updates"), "policy", List.of("reads"));
  private static final Collection<String> DEPENDS_ON_ATTRIBUTES = List.of("reads");

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
            "commands",
            new Glob("src/main/design", "**/*.command"),
            new SewFormat(),
            Command.class),
        new Input<>(
            "events", new Glob("src/main/design", "**/*.event"), new SewFormat(), Event.class),
        new Input<>(
            "policies", new Glob("src/main/design", "**/*.policy"), new SewFormat(), Policy.class),
        new Input<>(
            "readModels",
            new Glob("src/main/design", "**/*.readModel"),
            new SewFormat(),
            ReadModel.class),
        new Input<>(
            "users", new Glob("src/main/stakeholders", "**/*.user"), new SewFormat(), User.class));
  }

  @Override
  public List<Output> getOutputs() {
    return List.of(
        new Output(new Glob(OUTPUT_PATH, "*.html")), new Output(new Glob(OUTPUT_PATH, "*.png")));
  }

  @Override
  protected void validate(
      InputSource source, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    inputs
        .get("useCases", UseCase.class)
        .forEach(
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
  }

  private void validateScenario(
      Location location,
      List<Pointer> steps,
      ResolvedInputs inputs,
      Collection<Diagnostic> diagnostics) {
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
                  (name, reference) -> {
                    validateStepReference(reference, inputs, diagnostics, stepLocation);
                    var allowed = ALLOWED_ATTRIBUTES.getOrDefault(step.getType(), emptyList());
                    if (!allowed.contains(name)) {
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
              stepLocation));
    }
  }

  private void validateGrammar(
      Location location, List<Pointer> steps, Collection<Diagnostic> diagnostics) {
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
                var image = build(scenario, sink, diagnostics);
                writer.printf(
                    "    <img src=\"%s\"/>%n",
                    report.toUri().resolve(".").normalize().relativize(image.toUri()));
                if (!scenario.getSteps().isEmpty()) {
                  writer.println("    <ol>");
                  writer.print("      <li>");
                  writer.print(
                      String.join(
                          "</li>%s      <li>".formatted(System.lineSeparator()),
                          describeSteps(scenario.getSteps(), inputs)));
                  writer.println("</li>");
                  writer.println("    </ol>");
                }
              });
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
  }

  private OutputSink build(
      UseCase.Scenario scenario, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var result = sink.select(scenario.getName() + ".png");
    try {
      var image = render(scenario);
      try (var output = result.open()) {
        ImageIO.write(image, "PNG", output);
      }
    } catch (IOException e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
    return result;
  }

  private RenderedImage render(UseCase.Scenario scenario) {
    var graph = toGraph(scenario);
    var image = mxCellRenderer.createBufferedImage(graph, null, 1, null, true, null);
    var result =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    result.getGraphics().drawImage(image, 0, 0, null);
    return result;
  }

  private mxGraph toGraph(UseCase.Scenario scenario) {
    var result = new mxGraph();
    result.getModel().beginUpdate();
    try {
      var verticesByStep = new HashMap<Pointer, Object>();
      var from =
          new AtomicReference<>(addVertex(result, scenario.getSteps().getFirst(), verticesByStep));
      var first = from.get();
      scenario.getSteps().stream()
          .skip(1)
          .forEach(
              step -> {
                var to = addVertex(result, step, verticesByStep);
                result.insertEdge(result.getDefaultParent(), null, "", from.get(), to);
                from.set(to);

                step.getAttributes()
                    .forEach(
                        (name, reference) -> {
                          var begin = from.get();
                          var end = addVertex(result, reference, verticesByStep);
                          if (DEPENDS_ON_ATTRIBUTES.contains(name)) {
                            var node = begin;
                            begin = end;
                            end = node;
                          }
                          result.insertEdge(result.getDefaultParent(), null, "", begin, end);
                        });
              });

      var layout = new mxHierarchicalLayout(result, 7) { // left-to-right

            @Override
            public List<Object> findRoots(Object parent, Set<Object> vertices) {
              return List.of(first);
            }
          };
      layout.setInterRankCellSpacing(ICON_SIZE / 2.0);
      layout.setIntraCellSpacing(ICON_SIZE / 4.0);
      layout.execute(result.getDefaultParent());
    } finally {
      result.getModel().endUpdate();
    }

    return result;
  }

  private Object addVertex(mxGraph graph, Pointer step, Map<Pointer, Object> verticesByStep) {
    if (verticesByStep.containsKey(step)) {
      return verticesByStep.get(step);
    }
    var url = getClass().getClassLoader().getResource("resin/" + step.getType() + ".png");
    if (url == null) {
      throw new IllegalArgumentException("Icon not found for " + step.getType());
    }
    var result =
        graph.insertVertex(
            graph.getDefaultParent(),
            null,
            step.getId(),
            0,
            0,
            ICON_SIZE,
            ICON_SIZE,
            "shape=image;image=%s;verticalLabelPosition=bottom;verticalAlign=top;fontColor=#6482B9"
                .formatted(url.toExternalForm()));
    verticesByStep.put(step, result);
    return result;
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
          result.add(description.get());
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

  @Getter(AccessLevel.PROTECTED)
  @RequiredArgsConstructor
  private abstract static class Actor {

    private final String name;
    @Setter private List<NamedObject> input;

    public static Optional<Actor> from(NamedObject step) {
      if (isType(step, User.class)) {
        var userName = step instanceof User user ? user.getDisplay() : step.getName();
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

    protected <T extends NamedObject> String friendlyName(
        NamedObject source, Class<T> type, Function<T, String> extractor) {
      return type.isInstance(source) ? extractor.apply(type.cast(source)) : source.getName();
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
        return Optional.empty();
      }

      private String userIssues(String command) {
        return "The %s%s %s.".formatted(initLower(getName()), describeInputs(), initLower(command));
      }

      private String describeInputs() {
        if (getInput().isEmpty()) {
          return "";
        }
        var input = getInput().getFirst();
        var readModelText =
            input instanceof ReadModel readModel ? readModel.getDisplay() : input.getName();
        return ", looking at the %s,".formatted(initLower(readModelText));
      }
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
          return "%s responds with %s.".formatted(getName(), initLower(response));
        }
        return actions.isEmpty() ? null : "%s does nothing.".formatted(getName());
      }

      private String describeActions() {
        var last = actions.getLast();
        if (isType(last, Command.class)) {
          var command = friendlyName(last, Command.class, Command::getDisplay);
          return "%s %s.".formatted(getName(), command);
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
