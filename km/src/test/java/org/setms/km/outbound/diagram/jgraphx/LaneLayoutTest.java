package org.setms.km.outbound.diagram.jgraphx;

import static org.assertj.core.api.Assertions.assertThat;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class LaneLayoutTest {

  private final mxGraph graph = new mxGraph();
  private final mxGraphLayout layout = new LaneLayout(graph);

  @Test
  void shouldRenderSinglePathAsOneLane() {
    var v1 = addVertex("Ape");
    var v2 = addVertex("Bear");
    insertEdge(v1, v2, "Cheetah");

    assertThatLayoutLooksGood();
    assertThatVerticesAreInOneLane();
  }

  private mxCell addVertex(String text) {
    return (mxCell) graph.insertVertex(graph.getDefaultParent(), null, text, 0, 0, 52, 80);
  }

  private void insertEdge(Object from, Object to, String text) {
    graph.insertEdge(graph.getDefaultParent(), null, text, from, to);
  }

  private void assertThatLayoutLooksGood() {
    layoutGraph();

    assertThatNoOverlap();
  }

  private void layoutGraph() {
    layout.execute(graph.getDefaultParent());
  }

  private void assertThatNoOverlap() {
    var cells =
        Arrays.stream(graph.getChildCells(graph.getDefaultParent()))
            .map(mxCell.class::cast)
            .toList();
    cells.forEach(cell -> assertThatNoOverlapExistsBetween(cell, cells));
  }

  private Rectangle getBounds(mxCell cell) {
    return cell.getGeometry().getRectangle();
  }

  private void assertThatNoOverlapExistsBetween(mxCell cell, Collection<mxCell> cells) {
    cells.stream()
        .filter(candidate -> candidate != cell)
        .forEach(other -> assertThatNoOverlapExistsBetween(cell, other));
  }

  private void assertThatNoOverlapExistsBetween(mxCell c1, mxCell c2) {
    assertThat(getBounds(c1).intersects(getBounds(c2)))
        .as("Expected no overlap between %s and %s", c1, c2)
        .isFalse();
  }

  private void assertThatVerticesAreInOneLane() {
    assertThatVerticesAreInHorizontalLane(getVertices());
  }

  private Collection<mxCell> getVertices() {
    return Arrays.stream(graph.getChildVertices(graph.getDefaultParent()))
        .map(mxCell.class::cast)
        .toList();
  }

  private void assertThatVerticesAreInHorizontalLane(Collection<mxCell> cells) {
    var bounds = cells.stream().map(this::getBounds).toList();
    assertThat(bounds.stream().map(Rectangle::getY).distinct().count())
        .as("# distinct Y coordinates")
        .isEqualTo(1);

    var xCoordinates = bounds.stream().map(Rectangle::getX).toList();
    var sorted = new ArrayList<>(xCoordinates);
    Collections.sort(sorted);
    assertThat(xCoordinates).as("Vertices are placed from left to right").isEqualTo(sorted);
    var deltas = new LinkedHashSet<Integer>();
    for (var i = 1; i < xCoordinates.size(); i++) {
      var delta = (int) (xCoordinates.get(i) - xCoordinates.get(i - 1));
      deltas.add(delta + delta % 2);
    }
    assertThat(deltas).as("Vertices are evenly spaced").hasSize(1);
  }

  @Test
  void shouldRenderTwoConvergingPathsAsOneLane() {
    var v1 = addVertex("Ape");
    var v2 = addVertex("Bear");
    insertEdge(v1, v2, "Cheetah");
    var v3 = addVertex("Dingo");
    insertEdge(v3, v2, "Elephant");

    assertThatLayoutLooksGood();
    assertThatVerticesAreInOneLane();
  }

  @Test
  void shouldRenderTwoIndependentLanes() {
    var v1 = addVertex("Ape");
    var v2 = addVertex("Bear");
    insertEdge(v1, v2, "Cheetah");
    var v3 = addVertex("Dingo");
    var v4 = addVertex("Elephant");
    insertEdge(v3, v4, "Fox");

    assertThatLayoutLooksGood();
    assertThatVerticesAreInHorizontalLane(List.of(v1, v2));
    assertThatVerticesAreInHorizontalLane(List.of(v3, v4));
  }

  @Test
  void shouldRenderTwoLanesWithCommonStart() {
    var v1 = addVertex("Ape");
    var v2 = addVertex("Bear");
    insertEdge(v1, v2, "Cheetah");
    var v3 = addVertex("Dingo");
    var v4 = addVertex("Elephant");
    insertEdge(v3, v4, "Fox");
    insertEdge(v4, v2, "Giraffe");
    var v5 = addVertex("Hyena");
    var v6 = addVertex("Iguana");
    insertEdge(v1, v5, "Jaguar koalaleopardmule");
    insertEdge(v5, v6, "Nightingale");

    assertThatLayoutLooksGood();
  }
}
