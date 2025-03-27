package org.setms.sew.format;

import static java.util.stream.Collectors.joining;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
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

  @Override
  public String toString() {
    return "{ %s }"
        .formatted(
            valuesByKey.entrySet().stream()
                .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                .collect(joining(", ")));
  }
}
