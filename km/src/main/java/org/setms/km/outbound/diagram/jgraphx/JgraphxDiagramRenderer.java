package org.setms.km.outbound.diagram.jgraphx;

import static javax.swing.SwingConstants.NORTH;
import static javax.swing.SwingConstants.WEST;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import java.awt.image.BufferedImage;
import org.setms.km.domain.model.diagram.Arrow;
import org.setms.km.domain.model.diagram.BaseDiagramRenderer;
import org.setms.km.domain.model.diagram.Box;
import org.setms.km.domain.model.diagram.Diagram;
import org.setms.km.domain.model.diagram.IconBox;
import org.setms.km.domain.model.diagram.Orientation;
import org.setms.km.domain.model.diagram.ShapeBox;

public class JgraphxDiagramRenderer extends BaseDiagramRenderer {

  private static final int ICON_SIZE = 52;
  private static final int LINE_HEIGHT = 16;
  private static final String ICON_VERTEX_STYLE =
      "shape=image;image=%s;verticalLabelPosition=bottom;verticalAlign=top;fontColor=#6482B9;";

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
      layoutGraph(diagram.getOrientation(), context, result);
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
          case ShapeBox shapeBox -> addShapeVertex(shapeBox, context, graph);
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
        ICON_VERTEX_STYLE.formatted(icon.toExternalForm()));
  }

  private Object addShapeVertex(ShapeBox shapeBox, GraphContext context, mxGraph graph) {
    // TODO: Implement
    return null;
  }

  private void addEdge(Arrow arrow, GraphContext context, mxGraph graph) {
    graph.insertEdge(
        graph.getDefaultParent(),
        null,
        arrow.middleText(),
        context.vertexFor(arrow.from()),
        context.vertexFor(arrow.to()));
  }

  private void layoutGraph(Orientation orientation, GraphContext context, mxGraph graph) {
    var layout = new mxHierarchicalLayout(graph, convert(orientation));
    layout.setInterRankCellSpacing(2.0 * ICON_SIZE);
    layout.setIntraCellSpacing(context.getBoxHeight() - ICON_SIZE + LINE_HEIGHT);
    layout.execute(graph.getDefaultParent());
  }

  private int convert(Orientation orientation) {
    return switch (orientation) {
      case LEFT_TO_RIGHT -> WEST;
      case TOP_TO_BOTTOM -> NORTH;
    };
  }
}
