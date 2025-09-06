package org.setms.km.outbound.diagram.jgraphx;

import static javax.swing.SwingConstants.NORTH;
import static javax.swing.SwingConstants.WEST;
import static org.setms.km.domain.model.diagram.Placement.NEAR_FROM_VERTEX;
import static org.setms.km.domain.model.diagram.Placement.NEAR_TO_VERTEX;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;
import java.awt.image.BufferedImage;
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
  private static final int LINE_HEIGHT = 16;
  private static final int SHAPE_WIDTH = 120;
  private static final int SHAPE_HEIGHT = 60;
  private static final String COLOR = "#6482B9;";
  private static final String VERTEX_STYLE_ICON =
      "shape=image;image=%s;verticalLabelPosition=bottom;verticalAlign=top;fontColor=" + COLOR;
  private static final String VERTEX_STYLE_ELLIPSE =
      "shape=ellipse;fillColor=none;fontColor=" + COLOR;
  private static final String VERTEX_STYLE_RECTANGLE =
      "shape=rectangle;fillColor=none;fontColor=" + COLOR;
  private static final String VERTEX_STYLE_EDGE_POINT =
      "fontSize=16;resizable=0;movable=0;rotatable=0;shape=label;align=center;verticalAlign=middle;connectable=0;strokeColor=none;fillColor=none;fontStyle=1;fontColor="
          + COLOR;
  private static final String EDGE_STYLE_BIDIRECTIONAL = "endArrow: none";
  private static final String EDGE_STYLE_NORMAL = null;
  private static final int EDGE_POINT_SIZE = 20;
  private static final int MARGIN = 3;

  @Override
  public String getId() {
    return "jgraphx";
  }

  @Override
  public BufferedImage doRender(Diagram diagram, int numBoxTextLines) {
    return mxCellRenderer.createBufferedImage(
        toGraph(diagram, new GraphContext(ICON_SIZE + (numBoxTextLines - 1) * LINE_HEIGHT)),
        null,
        1,
        null,
        true,
        null);
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
        context.getBoxHeight(),
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
      Object edge, String text, Placement placement, GraphContext context, mxGraph graph) {
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

    graph.refresh();

    context.getEdgeLabels().forEach(placement -> positionEdgeLabel(graph, placement));
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
    result.setIntraCellSpacing(context.getBoxHeight() - ICON_SIZE + LINE_HEIGHT);
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

  private void positionEdgeLabel(mxGraph graph, EdgeLabel edgeLabel) {
    var edgeState = graph.getView().getState(edgeLabel.edge());
    if (edgeState == null || edgeState.getAbsolutePoints() == null) {
      return;
    }

    var points = edgeState.getAbsolutePoints();
    var base = edgeLabel.isPlacedNearFromVertex() ? points.get(0) : points.getLast();
    var next = edgeLabel.isPlacedNearFromVertex() ? points.get(1) : points.get(points.size() - 2);

    var height = edgeState.getLabelBounds().getHeight() / 2.0;
    var labelPoint = new mxPoint(base.getX() + height + MARGIN, base.getY() - height - MARGIN);

    var dx = next.getX() - base.getX();
    var dy = next.getY() - base.getY();
    var rad = Math.atan2(dy, dx);

    dx = labelPoint.getX() - base.getX();
    dy = labelPoint.getY() - base.getY();
    labelPoint.setX(base.getX() + dx * Math.cos(rad) - dy * Math.sin(rad));
    labelPoint.setY(base.getY() + dx * Math.sin(rad) + dy * Math.cos(rad));

    var geo = edgeLabel.label().getGeometry();
    geo.setX(labelPoint.getX() - height);
    geo.setY(labelPoint.getY() - height);

    /*
    var len = Math.max(1, Math.sqrt(dx * dx + dy * dy));
    dx /= len;
    dy /= len;
    var px = -dy;
    var py = dx;

    var offsetAlong = 12;
    var offsetPerpendicular = -5;
    var labelX = base.getX() + dx * offsetAlong + px * offsetPerpendicular;
    var labelY = base.getY() + dy * offsetAlong + py * offsetPerpendicular;
    var xOffset = edgeLabel.isPlacedNearFromVertex() ? 15 : 5;
    var yOffset = edgeLabel.isPlacedNearFromVertex() ? 8 : 18;

    var geo = edgeLabel.label().getGeometry();
    geo.setX(labelX - xOffset);
    geo.setY(labelY - yOffset);
    geo.setRelative(false);
    graph.getModel().setGeometry(edgeLabel.label(), geo);
     */
  }
}
