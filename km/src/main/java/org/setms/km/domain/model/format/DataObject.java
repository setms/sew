package org.setms.km.domain.model.format;

import static java.util.stream.Collectors.joining;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@SuppressWarnings("unchecked")
public abstract class DataObject<T extends DataObject<T>> implements DataItem {

  private final Map<String, DataItem> valuesByKey = new TreeMap<>();

  public T set(String key, DataItem value) {
    valuesByKey.put(key, value);
    return (T) this;
  }

  public <V extends DataItem> V property(String key, Class<V> type) {
    return type.cast(property(key));
  }

  public DataItem property(String key) {
    return valuesByKey.get(key);
  }

  public void properties(BiConsumer<String, DataItem> consumer) {
    valuesByKey.forEach(consumer);
  }

  public Set<String> propertyNames() {
    return valuesByKey.keySet();
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
