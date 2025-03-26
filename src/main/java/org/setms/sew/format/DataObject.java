package org.setms.sew.format;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@SuppressWarnings("unchecked")
public abstract class DataObject<T extends DataObject<T>> implements DataItem {

  private final Map<String, DataItem> valuesByKey = new LinkedHashMap<>();

  public T set(String key, DataItem value) {
    valuesByKey.put(key, value);
    return (T) this;
  }

  public void properties(BiConsumer<String, DataItem> consumer) {
    valuesByKey.forEach(consumer);
  }
}
