package org.setms.km.outbound.diagram.jgraphx;

import static java.util.stream.Collectors.joining;

import com.mxgraph.model.mxCell;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode
public class Path {

  private final List<mxCell> items = new ArrayList<>();

  public Path(mxCell... source) {
    Collections.addAll(items, source);
  }

  public Path(Path source) {
    items.addAll(source.items);
  }

  public Path(Collection<mxCell> source) {
    items.addAll(source);
  }

  public void add(mxCell cell) {
    items.add(cell);
  }

  public int size() {
    return items.size();
  }

  public mxCell getFirst() {
    return items.getFirst();
  }

  public mxCell getLast() {
    return items.getLast();
  }

  public mxCell get(int index) {
    return items.get(index);
  }

  public boolean contains(mxCell cell) {
    return items.contains(cell);
  }

  public List<mxCell> toList() {
    return items;
  }

  public Stream<mxCell> stream() {
    return items.stream();
  }

  public mxCell before(mxCell cell) {
    var index = items.indexOf(cell);
    return index < 1 ? null : items.get(index - 1);
  }

  Path join(Path path) {
    if (items.getLast() != path.items.getFirst()) {
      throw new IllegalArgumentException();
    }
    var result = new Path(this);
    result.items.addAll(path.items.subList(1, path.items.size()));
    return result;
  }

  public Path reverse() {
    return new Path(items.reversed());
  }

  @Override
  public String toString() {
    return items.stream().map(this::cellToString).collect(joining("--"));
  }

  private String cellToString(mxCell cell) {
    return "%s{%s,(%.0f, %.0f)}"
        .formatted(
            cell.getValue(), cell.getId(), cell.getGeometry().getX(), cell.getGeometry().getY());
  }
}
