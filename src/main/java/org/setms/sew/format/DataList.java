package org.setms.sew.format;

import static java.util.Collections.addAll;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class DataList implements DataItem {

  private final List<DataItem> values = new ArrayList<>();

  public DataList add(Collection<DataItem> values) {
    this.values.addAll(values);
    return this;
  }

  public DataList add(DataItem... values) {
    addAll(this.values, values);
    return this;
  }

  public int size() {
    return values.size();
  }

  public DataItem getFirst() {
    return values.getFirst();
  }

  public <T> Stream<T> map(Function<DataItem, T> mapper) {
    return values.stream().map(mapper);
  }

  public boolean hasItems() {
    return !values.isEmpty();
  }

  @Override
  public String toString() {
    return "[ %s ]".formatted(values.stream().map(Object::toString).collect(joining(", ")));
  }
}
