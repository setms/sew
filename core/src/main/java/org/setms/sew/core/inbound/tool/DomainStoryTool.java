package org.setms.sew.core.inbound.tool;

import static org.setms.sew.core.domain.model.format.Strings.initLower;
import static org.setms.sew.core.domain.model.format.Strings.toFriendlyName;
import static org.setms.sew.core.domain.model.tool.Level.ERROR;
import static org.setms.sew.core.inbound.tool.Inputs.domainStories;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.view.mxGraph;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.domainstory.DomainStory;
import org.setms.sew.core.domain.model.sdlc.domainstory.Sentence;
import org.setms.sew.core.domain.model.tool.*;

public class DomainStoryTool extends Tool {

  private static final int ICON_SIZE = 52;
  private static final int MAX_TEXT_LENGTH = ICON_SIZE / 4;
  private static final int LINE_HEIGHT = 16;
  private static final String VERTEX_STYLE =
      "shape=image;image=%s;verticalLabelPosition=bottom;verticalAlign=top;fontColor=#6482B9;";

  @Override
  public List<Input<?>> getInputs() {
    return List.of(domainStories());
  }

  @Override
  public List<Output> getOutputs() {
    return List.of();
  }

  @Override
  public void build(ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var domainStories = inputs.get(DomainStory.class);
    var reportSink = sink.select("reports/domainStories");
    domainStories.forEach(domainStory -> build(domainStory, reportSink, diagnostics));
  }

  private void build(DomainStory domainStory, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var report = sink.select(domainStory.getName() + ".html");
    try (var writer = new PrintWriter(report.open())) {
      writer.println("<html>");
      writer.println("  <body>");
      writer.printf("    <h1>%s</h1>%n", toFriendlyName(domainStory.getName()));
      writer.printf("    <p>%s</p>%n", domainStory.getDescription());
      build(domainStory, toGraph(domainStory.getSentences()), sink, diagnostics)
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

  private Map<Pointer, String> wrappedVertexTextsFor(List<Sentence> sentences) {
    var result = new HashMap<Pointer, String>();
    sentences.forEach(
        sentence ->
            sentence.getParts().stream()
                .filter(p -> !p.isType("activity"))
                .forEach(part -> result.put(part, wrap(part.getId(), MAX_TEXT_LENGTH))));
    return result;
  }

  @SuppressWarnings("StringConcatenationInLoop")
  private int ensureSameNumberOfLinesFor(Map<Pointer, String> textsByPointer) {
    var result = textsByPointer.values().stream().mapToInt(this::numLinesIn).max().orElse(1);
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

  private void addSentence(
      int index,
      Sentence sentence,
      Map<String, Object> actorNodesByName,
      Map<Pointer, String> vertexTexts,
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
                case "user" ->
                    addActor(
                        part,
                        "material/person",
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
                                firstActivity.getAndSet(false) ? "(%d)%n".formatted(index + 1) : "",
                                toFriendlyName(part.getId())));
                case "workObject" ->
                    addVertex(
                        part,
                        Optional.ofNullable(part.getAttributes().get("icon"))
                            .map(List::getFirst)
                            .map(p -> "%s/%s".formatted(p.getType(), initLower(p.getId())))
                            .orElse("material/workObject"),
                        vertexTexts,
                        height,
                        previousVertex,
                        activity,
                        graph);
                case "externalSystem" ->
                    addActor(
                        part,
                        "material/system",
                        actorNodesByName,
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
      Pointer part,
      String type,
      Map<String, Object> actorNodesByName,
      Map<Pointer, String> vertexTexts,
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
      Pointer part,
      String type,
      Map<Pointer, String> vertexTexts,
      int height,
      AtomicReference<Object> previousVertex,
      AtomicReference<String> activity,
      mxGraph graph) {
    var result = addVertex(type, vertexTexts.get(part), height, graph);
    addEdge(previousVertex, result, activity.get(), graph);
    return result;
  }

  private Object addVertex(String type, String name, int height, mxGraph graph) {
    var url = getClass().getClassLoader().getResource("domainStory/" + type + ".png");
    if (url == null) {
      throw new IllegalArgumentException("Icon not found for " + type);
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
    inputs.get(DomainStory.class).forEach(domainStory -> validate(domainStory, diagnostics));
  }

  private void validate(DomainStory domainStory, Collection<Diagnostic> diagnostics) {
    var location = new Location(domainStory);
    var sentences = domainStory.getSentences();
    if (sentences == null || sentences.isEmpty()) {
      diagnostics.add(new Diagnostic(ERROR, "Missing sentences", location));
      return;
    }
    sentences.forEach(sentence -> validate(sentence, location.plus(sentence), diagnostics));
  }

  private void validate(Sentence sentence, Location location, Collection<Diagnostic> diagnostics) {
    var parts = sentence.getParts();
    if (parts == null || parts.isEmpty()) {
      diagnostics.add(new Diagnostic(ERROR, "Missing parts", location));
      return;
    }
    if (parts.size() < 3) {
      diagnostics.add(
          new Diagnostic(
              ERROR, "Need at least 3 parts: actor, activity, and work object", location));
    }
    if (!isActor(parts.getFirst())) {
      diagnostics.add(
          new Diagnostic(
              ERROR,
              "Sentence must start with an actor",
              location.plus("parts", parts, parts.getFirst())));
    }
    for (var i = 1; i < parts.size() - 1; i++) {
      var part = parts.get(i);
      var partLocation = location.plus("parts", parts, part);
      if (isActor(part)) {
        diagnostics.add(
            new Diagnostic(
                ERROR, "Actors can only be at the beginning or end of a sentence", partLocation));
      }
      if (part.isType("activity") != (i % 2 == 1)) {
        diagnostics.add(
            new Diagnostic(
                ERROR, "Must separate actors and work objects using activities", partLocation));
      }
    }
  }

  private boolean isActor(Pointer pointer) {
    return pointer.isType("user") || pointer.isType("externalSystem");
  }
}
