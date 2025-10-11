package org.setms.km.outbound.diagram.jgraphx;

import com.mxgraph.model.mxCell;
import java.util.stream.Stream;

public record Lane(Path path) {

  mxCell getFirst() {
    return path.getFirst();
  }

  Stream<mxCell> stream() {
    return path.stream();
  }

  public int size() {
    return path.size();
  }

  public boolean contains(mxCell cell) {
    return path.contains(cell);
  }

  @Override
  public String toString() {
    return "Lane{%s}".formatted(path);
  }

  public mxCell before(mxCell cell) {
    return path.before(cell);
  }
}
