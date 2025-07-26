package org.setms.sew.core.inbound.tool;

import static java.util.function.Predicate.not;
import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.format.Strings.toFriendlyName;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.sew.core.inbound.tool.Inputs.*;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.view.mxGraph;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.*;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.sew.core.domain.model.sdlc.domainstory.DomainStory;
import org.setms.sew.core.domain.model.sdlc.domainstory.Sentence;
import org.setms.sew.core.domain.model.sdlc.usecase.Scenario;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;
import org.setms.sew.core.domain.services.DomainStoryToUseCase;
import org.setms.sew.core.inbound.format.sal.SalFormat;

public class DomainStoryTool extends BaseTool {

  private static final int ICON_SIZE = 52;
  private static final int MAX_TEXT_LENGTH = ICON_SIZE / 4;
  private static final int LINE_HEIGHT = 16;
  private static final String VERTEX_STYLE =
      "shape=image;image=%s;verticalLabelPosition=bottom;verticalAlign=top;fontColor=#6482B9;";
  private static final String CREATE_USE_CASE_SCENARIO = "usecase.scenario.create";
  private static final String OUTPUT_PATH = "reports/domainStories";

  @Override
  public List<Input<?>> getInputs() {
    return List.of(domainStories(), useCases());
  }

  @Override
  public Optional<Output> getOutputs() {
    return htmlIn(OUTPUT_PATH);
  }

  @Override
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var domainStories = inputs.get(DomainStory.class);
    var reportResource = resource.select(OUTPUT_PATH);
    domainStories.forEach(domainStory -> build(domainStory, reportResource, diagnostics));
  }

  private void build(
      DomainStory domainStory, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var report = resource.select(domainStory.getName() + ".html");
    try (var writer = new PrintWriter(report.writeTo())) {
      writer.println("<html>");
      writer.println("  <body>");
      writer.printf("    <h1>%s</h1>%n", toFriendlyName(domainStory.getName()));
      writer.printf("    <p>%s</p>%n", domainStory.getDescription());
      build(domainStory, toGraph(domainStory.getSentences()), resource, diagnostics)
          .ifPresent(
              image ->
                  writer.printf(
                      "    <img src=\"%s\"/>%n",
                      report.toUri().resolve(".").normalize().relativize(image.toUri())));
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private mxGraph toGraph(List<Sentence> sentences) {
    var result = new mxGraph();
    var actorNodesByName = new HashMap<String, Object>();
    var vertexTexts = wrappedVertexTextsFor(sentences);
    var numLines = ensureSameNumberOfLinesFor(vertexTexts);
    var height = ICON_SIZE + (numLines - 1) * LINE_HEIGHT;
    result.getModel().beginUpdate();
    try {
      for (var i = 0; i < sentences.size(); i++) {
        addSentence(i, sentences.get(i), actorNodesByName, vertexTexts, height, result);
      }
      layoutGraph(result, height);
    } finally {
      result.getModel().endUpdate();
    }

    return result;
  }

  private Map<Link, String> wrappedVertexTextsFor(List<Sentence> sentences) {
    var result = new HashMap<Link, String>();
    sentences.forEach(
        sentence ->
            sentence.getParts().stream()
                .filter(p -> !p.hasType("activity"))
                .forEach(part -> result.put(part, wrap(part.getId(), MAX_TEXT_LENGTH))));
    return result;
  }

  @SuppressWarnings("StringConcatenationInLoop")
  private int ensureSameNumberOfLinesFor(Map<Link, String> textsByPart) {
    var result = textsByPart.values().stream().mapToInt(this::numLinesIn).max().orElse(1);
    textsByPart.forEach(
        (part, text) -> {
          var addLines = result - numLinesIn(text);
          for (var i = 0; i < addLines; i++) {
            text += NL;
          }
          textsByPart.put(part, text);
        });
    return result;
  }

  private int numLinesIn(String text) {
    return text.split(NL).length;
  }

  private void addSentence(
      int index,
      Sentence sentence,
      Map<String, Object> actorNodesByName,
      Map<Link, String> vertexTexts,
      int height,
      mxGraph graph) {
    var firstActivity = new AtomicBoolean(true);
    var previousVertex = new AtomicReference<>();
    var activity = new AtomicReference<String>();
    sentence
        .getParts()
        .forEach(
            part -> {
              switch (part.getType()) {
                case "person" ->
                    addActor(
                        part,
                        "material/person",
                        actorNodesByName,
                        vertexTexts,
                        height,
                        previousVertex,
                        activity,
                        graph);
                case "people" ->
                    addActor(
                        part,
                        "material/group",
                        actorNodesByName,
                        vertexTexts,
                        height,
                        previousVertex,
                        activity,
                        graph);
                case "computerSystem" ->
                    addActor(
                        part,
                        "material/computer",
                        actorNodesByName,
                        vertexTexts,
                        height,
                        previousVertex,
                        activity,
                        graph);
                case "activity" ->
                    activity.set(
                        "%s%s"
                            .formatted(
                                firstActivity.getAndSet(false) ? "%c%n".formatted('①' + index) : "",
                                initLower(toFriendlyName(part.getId()))));
                case "workObject" ->
                    addVertex(
                        part,
                        Optional.ofNullable(part.getAttributes().get("icon"))
                            .map(List::getFirst)
                            .map(p -> "%s/%s".formatted(p.getType(), initLower(p.getId())))
                            .orElse("material/folder"),
                        vertexTexts,
                        height,
                        previousVertex,
                        activity,
                        graph);
                default ->
                    throw new UnsupportedOperationException("Can't render " + part.getType());
              }
            });
  }

  private void addActor(
      Link part,
      String type,
      Map<String, Object> actorNodesByName,
      Map<Link, String> vertexTexts,
      int height,
      AtomicReference<Object> previousVertex,
      AtomicReference<String> activity,
      mxGraph graph) {
    var vertex = actorNodesByName.get(part.getId());
    if (vertex == null) {
      vertex = addVertex(part, type, vertexTexts, height, previousVertex, activity, graph);
      actorNodesByName.put(part.getId(), vertex);
    } else {
      addEdge(previousVertex, vertex, activity.get(), graph);
    }
  }

  private Object addVertex(
      Link part,
      String type,
      Map<Link, String> vertexTexts,
      int height,
      AtomicReference<Object> previousVertex,
      AtomicReference<String> activity,
      mxGraph graph) {
    var result = addVertex(type, vertexTexts.get(part), height, graph);
    addEdge(previousVertex, result, activity.get(), graph);
    return result;
  }

  private Object addVertex(String type, String name, int height, mxGraph graph) {
    var url = loadIcon(type);
    if (url == null) {
      url = loadIcon("material/questionMark");
    }
    return graph.insertVertex(
        graph.getDefaultParent(),
        null,
        toFriendlyName(name),
        0,
        0,
        ICON_SIZE,
        height,
        VERTEX_STYLE.formatted(url.toExternalForm()));
  }

  private URL loadIcon(String type) {
    return getClass().getClassLoader().getResource("domainStory/" + type + ".png");
  }

  private void addEdge(
      AtomicReference<Object> fromReference, Object to, String text, mxGraph graph) {
    var from = fromReference.getAndSet(to);
    if (from == null) {
      return;
    }
    graph.insertEdge(graph.getDefaultParent(), null, text, from, to);
  }

  private void layoutGraph(mxGraph graph, int height) {
    var layout = new mxHierarchicalLayout(graph, 7);
    layout.setInterRankCellSpacing(2.0 * ICON_SIZE);
    layout.setIntraCellSpacing(height - ICON_SIZE + LINE_HEIGHT);
    layout.execute(graph.getDefaultParent());
  }

  @Override
  protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var useCases = inputs.get(UseCase.class);
    inputs
        .get(DomainStory.class)
        .forEach(domainStory -> validate(domainStory, useCases, diagnostics));
  }

  private void validate(
      DomainStory domainStory, Collection<UseCase> useCases, Collection<Diagnostic> diagnostics) {
    if (useCases.stream()
        .flatMap(UseCase::scenarios)
        .map(Scenario::getElaborates)
        .filter(Objects::nonNull)
        .noneMatch(elaborates -> elaborates.pointsTo(domainStory))) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Not elaborated in use case scenario",
              domainStory.toLocation(),
              elaborationSuggestions(useCases)));
    }
  }

  private List<Suggestion> elaborationSuggestions(Collection<UseCase> useCases) {
    return Stream.concat(
            Stream.of(new Suggestion(CREATE_USE_CASE_SCENARIO, "Elaborate in new use case")),
            useCases.stream()
                .map(Artifact::getFullyQualifiedName)
                .map(
                    name ->
                        new Suggestion(
                            "%s.%s".formatted(CREATE_USE_CASE_SCENARIO, name),
                            "Elaborate in use case %s".formatted(name))))
        .toList();
  }

  @Override
  protected void apply(
      String suggestionCode,
      ResolvedInputs inputs,
      Location location,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
    if (suggestionCode.startsWith(CREATE_USE_CASE_SCENARIO)) {
      findDomainStory(inputs, location)
          .ifPresent(
              domainStory ->
                  elaborateInUseCase(
                      domainStory,
                      extractUseCaseNameFrom(suggestionCode)
                          .flatMap(name -> find(inputs.get(UseCase.class), name)),
                      resource,
                      diagnostics));
    } else {
      super.apply(suggestionCode, inputs, location, resource, diagnostics);
    }
  }

  private Optional<FullyQualifiedName> extractUseCaseNameFrom(String suggestionCode) {
    return Optional.of(suggestionCode)
        .map(code -> code.substring(CREATE_USE_CASE_SCENARIO.length()))
        .filter(not(String::isEmpty))
        .map(code -> code.substring(1))
        .map(FullyQualifiedName::new);
  }

  private Optional<UseCase> find(Collection<UseCase> useCases, FullyQualifiedName name) {
    return useCases.stream()
        .filter(useCase -> useCase.getFullyQualifiedName().equals(name))
        .findFirst();
  }

  private Optional<DomainStory> findDomainStory(ResolvedInputs inputs, Location location) {
    return inputs.get(DomainStory.class).stream()
        .filter(domainStory -> domainStory.starts(location))
        .findFirst();
  }

  private void elaborateInUseCase(
      DomainStory domainStory,
      Optional<UseCase> source,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
    try {
      var converter = new DomainStoryToUseCase();
      var useCase =
          source
              .map(uc -> converter.addScenarioFrom(domainStory, uc))
              .orElseGet(() -> converter.createUseCaseFrom(domainStory));
      var useCaseResource = resourceFor(useCase, resource);
      try (var output = useCaseResource.writeTo()) {
        new SalFormat().newBuilder().build(useCase, output);
      }
      diagnostics.add(resourceCreated(useCaseResource));
    } catch (Exception e) {
      addError(diagnostics, e.getMessage());
    }
  }
}
