package org.setms.km.outbound.diagram.jgraphx;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static javax.swing.SwingConstants.NORTH;
import static javax.swing.SwingConstants.WEST;
import static org.setms.km.domain.model.diagram.Placement.NEAR_FROM_VERTEX;
import static org.setms.km.domain.model.diagram.Placement.NEAR_TO_VERTEX;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.setms.km.domain.model.diagram.Arrow;
import org.setms.km.domain.model.diagram.BaseDiagramRenderer;
import org.setms.km.domain.model.diagram.Box;
import org.setms.km.domain.model.diagram.Diagram;
import org.setms.km.domain.model.diagram.IconBox;
import org.setms.km.domain.model.diagram.Layout;
import org.setms.km.domain.model.diagram.Orientation;
import org.setms.km.domain.model.diagram.Placement;
import org.setms.km.domain.model.diagram.Shape;
import org.setms.km.domain.model.diagram.ShapeBox;

public class JgraphxDiagramRenderer extends BaseDiagramRenderer {

  private static final int ICON_SIZE = 52;
  private static final int LINE_HEIGHT = 13;
  private static final int SHAPE_WIDTH = 120;
  private static final int SHAPE_HEIGHT = 60;
  private static final String COLOR = "#6482B9;";
  private static final String VERTEX_STYLE_ICON =
      "shape=label;strokeColor=none;image=%s;imageWidth="
          + ICON_SIZE
          + ";imageHeight="
          + ICON_SIZE
          + ";imageAlign=center;imageVerticalAlign=top;verticalLabelPosition=middle;align=center;verticalAlign=bottom;"
          + "spacing=-1;margin=0;fillColor=none;fontColor="
          + COLOR;
  private static final String VERTEX_STYLE_ELLIPSE =
      "shape=ellipse;fillColor=none;fontColor=" + COLOR;
  private static final String VERTEX_STYLE_RECTANGLE =
      "shape=rectangle;fillColor=none;fontColor=" + COLOR;
  private static final String VERTEX_STYLE_EDGE_POINT =
      "fontSize=16;resizable=0;movable=0;rotatable=0;shape=label;align=center;verticalAlign=middle;connectable=0;strokeColor=none;fillColor=none;fontStyle=1;fontColor="
          + COLOR;
  private static final String EDGE_STYLE_NORMAL = "align=center;verticalAlign=middle";
  private static final String EDGE_STYLE_BIDIRECTIONAL = EDGE_STYLE_NORMAL + ";endArrow: none";
  private static final int EDGE_POINT_SIZE = 20;
  private static final int MARGIN = 5;

  @Override
  public String getId() {
    return "jgraphx";
  }

  @Override
  public BufferedImage doRender(Diagram diagram, int numBoxTextLines) {
    var graph = toGraph(diagram, new GraphContext(numBoxTextLines * LINE_HEIGHT));
    return mxCellRenderer.createBufferedImage(
        graph, graph.getChildCells(graph.getDefaultParent()), 1, null, true, null);
  }

  private mxGraph toGraph(Diagram diagram, GraphContext context) {
    var result = new mxGraph();
    result.getModel().beginUpdate();
    try {
      buildGraph(diagram, context, result);
      layoutGraph(diagram.getLayout(), diagram.getOrientation(), context, result);
    } finally {
      result.getModel().endUpdate();
    }
    return result;
  }

  private void buildGraph(Diagram diagram, GraphContext context, mxGraph graph) {
    diagram.getBoxes().forEach(box -> addVertex(box, context, graph));
    diagram.getArrows().forEach(arrow -> addEdge(arrow, context, graph));
  }

  private void addVertex(Box box, GraphContext context, mxGraph graph) {
    var vertex =
        switch (box) {
          case IconBox iconBox -> addIconVertex(iconBox, context, graph);
          case ShapeBox shapeBox -> addShapeVertex(shapeBox, graph);
        };
    context.associate(box, vertex);
  }

  private Object addIconVertex(IconBox box, GraphContext context, mxGraph graph) {
    var icon = loadIcon(box.getIconPath(), box.getFallbackPath());
    return graph.insertVertex(
        graph.getDefaultParent(),
        null,
        box.getText(),
        0,
        0,
        ICON_SIZE,
        ICON_SIZE + context.getBoxLabelHeight(),
        VERTEX_STYLE_ICON.formatted(icon.toExternalForm()));
  }

  private Object addShapeVertex(ShapeBox box, mxGraph graph) {
    return graph.insertVertex(
        graph.getDefaultParent(),
        null,
        box.getText(),
        0,
        0,
        SHAPE_WIDTH,
        SHAPE_HEIGHT,
        styleFor(box.getShape()));
  }

  private String styleFor(Shape shape) {
    return switch (shape) {
      case RECTANGLE -> VERTEX_STYLE_RECTANGLE;
      case ELLIPSE -> VERTEX_STYLE_ELLIPSE;
    };
  }

  private void addEdge(Arrow arrow, GraphContext context, mxGraph graph) {
    var edge =
        (mxCell)
            graph.insertEdge(
                graph.getDefaultParent(),
                null,
                arrow.middleText(),
                context.vertexFor(arrow.from()),
                context.vertexFor(arrow.to()),
                arrow.bidirectional() ? EDGE_STYLE_BIDIRECTIONAL : EDGE_STYLE_NORMAL);
    if (arrow.fromText() != null) {
      addEdgeLabel(edge, arrow.fromText(), NEAR_FROM_VERTEX, context, graph);
    }
    if (arrow.toText() != null) {
      addEdgeLabel(edge, arrow.toText(), NEAR_TO_VERTEX, context, graph);
    }
  }

  private void addEdgeLabel(
      mxCell edge, String text, Placement placement, GraphContext context, mxGraph graph) {
    context.addEdgeLabel(edge, createEdgeEndpointLabel(text, graph), placement);
  }

  private mxCell createEdgeEndpointLabel(String text, mxGraph graph) {
    return (mxCell)
        graph.insertVertex(
            graph.getDefaultParent(),
            null,
            text,
            0,
            0,
            EDGE_POINT_SIZE,
            EDGE_POINT_SIZE,
            VERTEX_STYLE_EDGE_POINT);
  }

  private void layoutGraph(
      Layout layout, Orientation orientation, GraphContext context, mxGraph graph) {
    newLayoutFor(layout, orientation, context, graph).execute(graph.getDefaultParent());

    // Validate to get vertex positions
    graph.getView().invalidate();
    graph.getView().validate();

    placeEdgeLabels(context, graph);

    graph.refresh();
  }

  private mxIGraphLayout newLayoutFor(
      Layout layout, Orientation orientation, GraphContext context, mxGraph graph) {
    return switch (layout) {
      case DEFAULT -> newHierarchicalLayout(orientation, context, graph);
      case LANE -> newLaneLayout(graph);
    };
  }

  private mxIGraphLayout newHierarchicalLayout(
      Orientation orientation, GraphContext context, mxGraph graph) {
    var result = new mxHierarchicalLayout(graph, convert(orientation));
    result.setInterRankCellSpacing(2.0 * ICON_SIZE);
    result.setIntraCellSpacing(context.getBoxLabelHeight());
    return result;
  }

  private int convert(Orientation orientation) {
    return switch (orientation) {
      case LEFT_TO_RIGHT -> WEST;
      case TOP_TO_BOTTOM -> NORTH;
    };
  }

  private mxIGraphLayout newLaneLayout(mxGraph graph) {
    return new LaneLayout(graph);
  }

  private void placeEdgeLabels(GraphContext context, mxGraph graph) {
    context.getEdgeLabels().forEach(placement -> placeEdgeLabel(graph, placement));
    var leftOfEdge = new AtomicBoolean(false);
    Arrays.stream(graph.getChildEdges(graph.getDefaultParent()))
        .map(mxCell.class::cast)
        .forEach(edge -> placeEdgeMiddleLabel(graph, edge, leftOfEdge));
  }

  private void placeEdgeLabel(mxGraph graph, EdgeLabel edgeLabel) {
    var edgeState = graph.getView().getState(edgeLabel.edge());
    if (edgeState == null || edgeState.getAbsolutePoints() == null) {
      return;
    }

    var points = edgeState.getAbsolutePoints();
    var base = edgeLabel.isPlacedNearFromVertex() ? points.get(0) : points.getLast();
    var next = edgeLabel.isPlacedNearFromVertex() ? points.get(1) : points.get(points.size() - 2);

    var bounds = edgeLabel.label().getGeometry();
    var labelPoint = new mxPoint(base.getX(), base.getY());

    var dx = next.getX() - base.getX();
    var dy = next.getY() - base.getY();
    var factor = 0.1 * (dx != 0 ? sqrt(abs(dy / dx)) : 1);
    labelPoint.setX(labelPoint.getX() + dx * factor);
    labelPoint.setY(labelPoint.getY() + dy * factor);

    var rad = -atan2(dy, dx);
    var tx = -MARGIN;
    var ty = -0.5 * bounds.getHeight() - MARGIN;
    labelPoint.setX(labelPoint.getX() + tx * cos(rad) - ty * sin(rad));
    labelPoint.setY(labelPoint.getY() + tx * sin(rad) + ty * cos(rad));

    if (dx > 0) {
      dx = MARGIN;
    } else {
      dx = -edgeLabel.edge().getSource().getGeometry().getWidth() + 3 * MARGIN;
    }

    var geo = edgeLabel.label().getGeometry();
    geo.setX(labelPoint.getX() + dx);
    geo.setY(labelPoint.getY());
  }

  private void placeEdgeMiddleLabel(mxGraph graph, mxCell edge, AtomicBoolean leftOnEdge) {
    var edgeState = graph.getView().getState(edge);
    if (edgeState == null || edgeState.getAbsolutePoints() == null) {
      return;
    }
    var source = edgeState.getAbsolutePoint(0);
    var target = edgeState.getAbsolutePoint(edgeState.getAbsolutePointCount() - 1);
    var bounds = edgeState.getLabelBounds();

    var dx = target.getX() - source.getX();
    var dy = target.getY() - source.getY();

    var offset = new mxPoint();
    if (abs(dy) < 0.1) {
      offset.setY(-MARGIN);
    } else if (abs(dx) < 0.1) {
      offset.setX(0.5 * bounds.getWidth());
      offset.setY(MARGIN);
    } else if (leftOnEdge.getAndSet(!leftOnEdge.get())) {
      offset.setX(-0.5 * bounds.getWidth());
    } else {
      offset.setX(0.5 * bounds.getWidth());
    }

    var geo = (mxGeometry) edge.getGeometry().clone();
    if (geo.isRelative() || !geo.getOffset().equals(offset)) {
      geo.setRelative(false);
      geo.setOffset(offset);
      edge.setGeometry(geo);
    }
  }
}
