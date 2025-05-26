package org.setms.sew.core.domain.model.format;

import static org.setms.sew.core.domain.model.format.Strings.initUpper;
import static org.setms.sew.core.domain.model.format.Validation.validate;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.atteo.evo.inflector.English;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;

public interface Parser {

  default <T extends NamedObject> T parse(InputStream input, Class<T> type, boolean validate)
      throws IOException {
    return convert(parse(input), type, validate);
  }

  RootObject parse(InputStream input) throws IOException;

  default <T extends NamedObject> T convert(RootObject object, Class<T> type, boolean validate) {
    if (!object.getType().equalsIgnoreCase(type.getSimpleName())) {
      throw new IllegalArgumentException(
          "Can't parse %s from %s".formatted(type.getName(), object.getType()));
    }
    var result = parseNamedObject(object, type, object.getScope(), object.getName(), validate);
    if (validate) {
      try {
        validate(result);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("%s: %s".formatted(result.getName(), e.getMessage()), e);
      }
    }
    return result;
  }

  default <T extends NamedObject> T parseNamedObject(
      DataObject<?> source, Class<T> type, String scope, Object name, boolean validate) {
    try {
      var result =
          type.getConstructor(FullyQualifiedName.class)
              .newInstance(new FullyQualifiedName("%s.%s".formatted(scope, name)));
      setProperties(source, result, validate);
      return result;
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException(e);
    }
  }

  default void setProperties(DataObject<?> source, Object target, boolean validate) {
    source.properties(
        (name, value) -> setProperty(name, convert(name, value, target, validate), target));
  }

  default Object convert(String name, DataItem value, Object target, boolean validate) {
    return switch (value) {
      case null -> null;
      case DataString string -> string.getValue();
      case DataList list ->
          list.map(item -> convert(name, item, target, validate)).filter(Objects::nonNull).toList();
      case NestedObject object -> createObject(object, name, target, validate);
      case Reference reference -> {
        var attributes = new HashMap<String, List<Pointer>>();
        reference
            .getAttributes()
            .forEach(
                (key, references) ->
                    attributes.put(
                        key,
                        references.stream()
                            .map(ref -> new Pointer(ref.getType(), ref.getId()))
                            .toList()));
        yield new Pointer(reference.getType(), reference.getId(), attributes);
      }
      default ->
          throw new UnsupportedOperationException(
              "Unexpected value of type " + value.getClass().getSimpleName());
    };
  }

  @SuppressWarnings("unchecked")
  default NamedObject createObject(
      NestedObject source, String name, Object parent, boolean validate) {
    return Arrays.stream(parent.getClass().getClasses())
        .filter(NamedObject.class::isAssignableFrom)
        .filter(c -> matchesName(name, c.getSimpleName()))
        .map(c -> (Class<NamedObject>) c)
        .findFirst()
        .map(type -> parseNamedObject(source, type, name, source.getName(), validate))
        .orElse(null);
  }

  default boolean matchesName(String name, String candidate) {
    return name.equalsIgnoreCase(candidate)
        || name.equalsIgnoreCase(English.plural(candidate))
        || English.plural(name).equalsIgnoreCase(candidate);
  }

  default void setProperty(String name, Object targetValue, Object target) {
    var setter = "set%s".formatted(initUpper(name));
    try {
      var method = findSetter(setter, targetValue, target.getClass());
      if (method != null) {
        if (Collection.class.isAssignableFrom(method.getParameters()[0].getType())
            && targetValue != null
            && !method.getParameters()[0].getType().isAssignableFrom(targetValue.getClass())) {
          method.invoke(target, toCollection(targetValue, method.getParameters()[0].getType()));
        } else {
          method.invoke(target, targetValue);
        }
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException(
          "Failed to set %s.%s".formatted(target.getClass(), name), e);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  default Collection<Object> toCollection(Object targetValue, Class<?> type) {
    if (List.class.isAssignableFrom(type)) {
      return targetValue instanceof Collection collection
          ? new ArrayList<>(collection)
          : List.of(targetValue);
    }
    if (Set.class.isAssignableFrom(type)) {
      return targetValue instanceof Collection collection
          ? new LinkedHashSet<>(collection)
          : Set.of(targetValue);
    }
    throw new UnsupportedOperationException("Unsupported collection type " + type.getName());
  }

  default Method findSetter(String name, Object targetValue, Class<?> type) {
    var methods =
        Arrays.stream(type.getMethods()).filter(m -> matchesName(name, m.getName())).toList();
    if (methods.isEmpty()) {
      // Ignore unsupported property (Postel's Law)
      return null;
    }
    if (methods.size() == 1) {
      return methods.getFirst();
    }
    return methods.stream()
        .filter(m -> m.getParameterCount() == 1)
        .filter(
            m ->
                targetValue == null
                    || m.getParameters()[0].getType().isAssignableFrom(targetValue.getClass()))
        .findFirst()
        .orElse(null);
  }
}
