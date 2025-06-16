package org.setms.sew.core.inbound.tool;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingConstants;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.ddd.Domain;
import org.setms.sew.core.domain.model.sdlc.ddd.Subdomain;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.format.sew.SewFormat;

@SuppressWarnings("unused") // At some point, we'll want a context map
public class ContextMapTool extends Tool {

  private static final String OUTPUT_PATH = "build/reports/domains";
  private static final String VERTEX_STYLE = "shape=ellipse;fontColor=#6482B9;fillColor=none;";
  private static final String EDGE_POINT_STYLE =
      """
      fontSize=9;resizable=0;movable=0;rotatable=0;\
      shape=label;align=center;verticalAlign=middle;connectable=0;\
      strokeColor=none;fillColor=none;fontColor=#6482B9;""";
  private static final String EDGE_STYLE = "endArrow=none";
  private static final int MAX_TEXT_LENGTH = 15;

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>(
            "domains",
            new Glob("src/main/requirements", "**/*.domain"),
            new SewFormat(),
            Domain.class),
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
    var output = sink.select("reports/domains");
    inputs.get(Domain.class).forEach(domain -> build(domain, output, diagnostics));
  }

  private void build(Domain domain, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var report = sink.select(domain.getName() + ".html");
    try (var writer = new PrintWriter(report.open())) {
      writer.println("<html>");
      writer.println("  <body>");
      var image = build(domain, toGraph(domain), sink, diagnostics);
      writer.printf(
          "    <img src=\"%s\" width=\"100%%\">%n",
          report.toUri().resolve(".").normalize().relativize(image.toUri()));
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private mxGraph toGraph(Domain domain) {
    var result = new mxGraph();
    var edgeLabelPositions = new HashMap<mxCell, EdgeLabelPlacement>();
    result.getModel().beginUpdate();
    try {
      buildGraph(domain, edgeLabelPositions, result);
      layoutGraph(edgeLabelPositions, result);
    } finally {
      result.getModel().endUpdate();
    }
    return result;
  }

  private void buildGraph(
      Domain domain, Map<mxCell, EdgeLabelPlacement> edgeLabelPositions, mxGraph graph) {
    var verticesBySubdomain = new HashMap<Subdomain, Object>();
    domain
        .getSubdomains()
        .forEach(subdomain -> verticesBySubdomain.put(subdomain, addVertex(subdomain, graph)));
    domain
        .getSubdomains()
        .forEach(
            source ->
                source
                    .dependsOn()
                    .forEach(
                        pointer ->
                            addEdge(
                                domain,
                                source,
                                pointer,
                                verticesBySubdomain,
                                graph,
                                edgeLabelPositions)));
  }

  private Object addVertex(Subdomain domain, mxGraph graph) {
    return graph.insertVertex(
        graph.getDefaultParent(),
        null,
        wrap(domain.getName(), MAX_TEXT_LENGTH),
        0,
        0,
        120,
        60,
        VERTEX_STYLE);
  }

  private void addEdge(
      Domain domain,
      Subdomain source,
      Pointer pointer,
      Map<Subdomain, Object> verticesBySubdomain,
      mxGraph graph,
      Map<mxCell, EdgeLabelPlacement> edgeLabelPositions) {
    pointer
        .resolveFrom(domain.getSubdomains())
        .ifPresent(
            target -> {
              var from = verticesBySubdomain.get(source);
              var to = verticesBySubdomain.get(target);
              var edge = graph.insertEdge(graph.getDefaultParent(), null, "", from, to, EDGE_STYLE);
              addEdgeLabels(edge, edgeLabelPositions, graph);
            });
  }

  private void addEdgeLabels(
      Object edge, Map<mxCell, EdgeLabelPlacement> edgeLabelPositions, mxGraph graph) {
    var upstreamLabel = createEdgeEndpointLabel(graph, "U");
    var downstreamLabel = createEdgeEndpointLabel(graph, "D");
    edgeLabelPositions.put(upstreamLabel, new EdgeLabelPlacement(edge, false));
    edgeLabelPositions.put(downstreamLabel, new EdgeLabelPlacement(edge, true));
  }

  private mxCell createEdgeEndpointLabel(mxGraph graph, String text) {
    return (mxCell)
        graph.insertVertex(graph.getDefaultParent(), null, text, 0, 0, 20, 20, EDGE_POINT_STYLE);
  }

  private void layoutGraph(Map<mxCell, EdgeLabelPlacement> edgeLabelPositions, mxGraph graph) {
    var layout = new mxHierarchicalLayout(graph, SwingConstants.WEST);
    layout.execute(graph.getDefaultParent());

    graph.refresh();

    var edgesByTarget = new HashMap<Object, List<mxCell>>();
    for (var placement : edgeLabelPositions.values()) {
      var edge = (mxCell) placement.edge();
      var target = edge.getTarget();
      edgesByTarget.computeIfAbsent(target, ignored -> new ArrayList<>()).add(edge);
    }

    edgeLabelPositions.forEach(
        (label, placement) ->
            positionEdgeLabel(graph, placement.edge(), label, placement.atSource()));
  }

  private void positionEdgeLabel(mxGraph graph, Object edge, mxCell label, boolean atSource) {
    var edgeState = graph.getView().getState(edge);
    if (edgeState == null || edgeState.getAbsolutePoints() == null) {
      return;
    }

    var points = edgeState.getAbsolutePoints();
    var base = atSource ? points.get(0) : points.getLast();
    var next = atSource ? points.get(1) : points.get(points.size() - 2);

    var dx = next.getX() - base.getX();
    var dy = next.getY() - base.getY();
    var len = Math.max(1, Math.sqrt(dx * dx + dy * dy));
    dx /= len;
    dy /= len;
    var px = -dy;
    var py = dx;

    var offsetAlong = 12;
    var offsetPerp = -5;
    var labelX = base.getX() + dx * offsetAlong + px * offsetPerp;
    var labelY = base.getY() + dy * offsetAlong + py * offsetPerp;
    var xOffset = atSource ? 15 : 5;
    var yOffset = atSource ? 8 : 18;

    var geo = label.getGeometry();
    geo.setX(labelX - xOffset);
    geo.setY(labelY - yOffset);
    geo.setRelative(false);
    graph.getModel().setGeometry(label, geo);
  }

  private record EdgeLabelPlacement(Object edge, boolean atSource) {}
}
