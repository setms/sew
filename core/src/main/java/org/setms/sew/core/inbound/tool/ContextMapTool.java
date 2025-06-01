package org.setms.sew.core.inbound.tool;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.view.mxGraph;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.setms.sew.core.domain.model.sdlc.ContextMap;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.UseCase;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.format.sew.SewFormat;

public class ContextMapTool extends Tool {

  private static final String OUTPUT_PATH = "build/reports/contextMaps";
  private static final String VERTEX_STYLE = "shape=ellipse;fontColor=#6482B9;";

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>(
            "contextMaps",
            new Glob("src/main/architecture", "**/*.contextMap"),
            new SewFormat(),
            ContextMap.class),
        new Input<>(
            "useCases",
            new Glob("src/main/requirements", "**/*.useCase"),
            new SewFormat(),
            UseCase.class));
  }

  @Override
  public List<Output> getOutputs() {
    return List.of(new Output(new Glob(OUTPUT_PATH, "*.png")));
  }

  @Override
  protected void build(ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var output = sink.select("reports/contextMaps");
    inputs
        .get("contextMaps", ContextMap.class)
        .forEach(contextMap -> build(contextMap, output, diagnostics));
  }

  private void build(ContextMap contextMap, OutputSink sink, Collection<Diagnostic> diagnostics) {
    build(contextMap, toGraph(contextMap), sink, diagnostics);
  }

  private mxGraph toGraph(ContextMap contextMap) {
    var result = new mxGraph();
    result.getModel().beginUpdate();
    try {
      buildGraph(contextMap, result);
      layoutGraph(result);
    } finally {
      result.getModel().endUpdate();
    }
    return result;
  }

  private void buildGraph(ContextMap contextMap, mxGraph graph) {
    var verticesByBoundedContext = new HashMap<ContextMap.BoundedContext, Object>();
    contextMap
        .getBoundedContexts()
        .forEach(
            boundedContext ->
                verticesByBoundedContext.put(boundedContext, addVertex(boundedContext, graph)));
    contextMap
        .getBoundedContexts()
        .forEach(
            source ->
                source
                    .dependsOn()
                    .forEach(
                        pointer ->
                            addEdge(contextMap, source, pointer, verticesByBoundedContext, graph)));
  }

  private Object addVertex(ContextMap.BoundedContext boundedContext, mxGraph graph) {
    return graph.insertVertex(
        graph.getDefaultParent(), null, boundedContext.getName(), 0, 0, 0, 0, VERTEX_STYLE);
  }

  private void addEdge(
      ContextMap contextMap,
      ContextMap.BoundedContext source,
      Pointer pointer,
      Map<ContextMap.BoundedContext, Object> verticesByBoundedContext,
      mxGraph graph) {
    pointer
        .resolveFrom(contextMap.getBoundedContexts())
        .ifPresent(
            target -> {
              var from = verticesByBoundedContext.get(source);
              var to = verticesByBoundedContext.get(target);
              graph.insertEdge(graph.getDefaultParent(), null, "D", from, to);
            });
  }

  private void layoutGraph(mxGraph graph) {
    var layout = new mxHierarchicalLayout(graph, 7);
    layout.execute(graph.getDefaultParent());
  }
}
