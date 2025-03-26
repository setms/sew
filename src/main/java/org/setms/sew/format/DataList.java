package org.setms.sew.format;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.addAll;

public class DataList implements DataItem {

  private final List<DataItem> values = new ArrayList<>();

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
}
