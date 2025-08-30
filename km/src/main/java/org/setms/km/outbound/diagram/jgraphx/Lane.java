package org.setms.km.outbound.diagram.jgraphx;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public record Lane(Path path) {

  mxCell getFirst() {
    return path.getFirst();
  }

  mxCell getSecond() {
    return path.get(1);
  }

  mxCell getLast() {
    return path.getLast();
  }

  Stream<mxCell> stream() {
    return path.stream();
  }

  public Set<mxCell> toSet() {
    return new LinkedHashSet<>(path.toList());
  }

  public int size() {
    return path.size();
  }

  public boolean contains(mxCell cell) {
    return path.contains(cell);
  }

  public double width() {
    return path.stream().mapToDouble(this::widthOf).sum();
  }

  private double widthOf(mxCell cell) {
    return cell.getGeometry().getWidth();
  }

  public double maxY() {
    return path.stream()
        .map(mxCell::getGeometry)
        .map(mxGeometry::getRectangle)
        .mapToDouble(r -> r.getY() + r.getHeight())
        .max()
        .orElse(0);
  }

  Lane skip(int n) {
    return new Lane(new Path(path.stream().skip(n).toList()));
  }

  @Override
  public @NotNull String toString() {
    return "Lane{%s}".formatted(path);
  }

  public mxCell before(mxCell cell) {
    return path.before(cell);
  }
}
