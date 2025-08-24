package org.setms.km.outbound.diagram.jgraphx;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public record Lane(List<mxCell> cells) {

  mxCell getFirst() {
    return cells.getFirst();
  }

  mxCell getSecond() {
    return cells.get(1);
  }

  mxCell getLast() {
    return cells.getLast();
  }

  Stream<mxCell> stream() {
    return cells.stream();
  }

  public Set<mxCell> toSet() {
    return new LinkedHashSet<>(cells);
  }

  public int size() {
    return cells.size();
  }

  public double width() {
    return cells.stream().mapToDouble(this::widthOf).sum();
  }

  private double widthOf(mxCell cell) {
    return cell.getGeometry().getWidth();
  }

  public double maxY() {
    return cells.stream()
        .map(mxCell::getGeometry)
        .map(mxGeometry::getRectangle)
        .mapToDouble(r -> r.getY() + r.getHeight())
        .max()
        .orElse(0);
  }

  Lane skip(int n) {
    return new Lane(cells.stream().skip(n).toList());
  }
}
