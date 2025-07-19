package org.setms.km.domain.model.artifact;

import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.HashSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class Enums<T extends Enum<T>> extends HashSet<T> {

  // Workaround for Java not storing the item type for a generic collection at runtime:
  // make that type explicit
  private final Class<T> type;

  public static <T extends Enum<T>> Enums<T> of(Class<T> type) {
    return new Enums<>(type);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static <T extends Enum<T>> Enums<T> of(Collection<T> items) {
    var result = new Enums<>(items.iterator().next().getClass());
    result.addAll(items);
    return result;
  }
}
