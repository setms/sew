package org.setms.sew.core.domain.model.format;

import static org.setms.sew.core.domain.model.format.Strings.initCap;
import static org.setms.sew.core.domain.model.format.Validation.validate;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.atteo.evo.inflector.English;
import org.setms.sew.core.domain.model.schema.FullyQualifiedName;
import org.setms.sew.core.domain.model.schema.NamedObject;
import org.setms.sew.core.domain.model.schema.Pointer;

public interface Parser {

  default <T extends NamedObject> T parse(InputStream input, Class<T> type) throws IOException {
    return convert(parse(input), type);
  }

  RootObject parse(InputStream input) throws IOException;

  default <T extends NamedObject> T convert(RootObject object, Class<T> type) {
    if (!object.getType().equalsIgnoreCase(type.getSimpleName())) {
      throw new IllegalArgumentException(
          "Can't parse %s from %s".formatted(type.getName(), object.getType()));
    }
    var result = parseNamedObject(object, type, object.getScope(), object.getName());
    try {
      validate(result);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("%s: %s".formatted(result.getName(), e.getMessage()), e);
    }
    return result;
  }

  default <T extends NamedObject> T parseNamedObject(
      DataObject<?> source, Class<T> type, String scope, Object name) {
    try {
      var result =
          type.getConstructor(FullyQualifiedName.class)
              .newInstance(new FullyQualifiedName("%s.%s".formatted(scope, name)));
      setProperties(source, result);
      return result;
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException(e);
    }
  }

  default void setProperties(DataObject<?> source, Object target) {
    source.properties((name, value) -> setProperty(name, convert(name, value, target), target));
  }

  default Object convert(String name, DataItem value, Object target) {
    return switch (value) {
      case DataString string -> string.getValue();
      case DataList list -> list.map(item -> convert(name, item, target)).toList();
      case NestedObject object -> createObject(object, name, target);
      case Reference reference -> new Pointer(reference.getType(), reference.getId());
      default ->
          throw new UnsupportedOperationException(
              "Unexpected value of type " + value.getClass().getSimpleName());
    };
  }

  @SuppressWarnings("unchecked")
  default NamedObject createObject(NestedObject source, String name, Object parent) {
    var type =
        Arrays.stream(parent.getClass().getClasses())
            .filter(NamedObject.class::isAssignableFrom)
            .filter(c -> matchesName(name, c.getSimpleName()))
            .map(c -> (Class<NamedObject>) c)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Can't find class " + name));
    return parseNamedObject(source, type, name, source.getName());
  }

  default boolean matchesName(String name, String candidate) {
    return name.equalsIgnoreCase(candidate)
        || name.equalsIgnoreCase(English.plural(candidate))
        || English.plural(name).equalsIgnoreCase(candidate);
  }

  default void setProperty(String name, Object targetValue, Object target) {
    var setter = "set%s".formatted(initCap(name));
    try {
      var method = findSetter(setter, targetValue, target.getClass());
      if (method != null) {
        if (Collection.class.isAssignableFrom(method.getParameters()[0].getType())
            && targetValue != null
            && !Collection.class.isAssignableFrom(targetValue.getClass())) {
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

  default Collection<Object> toCollection(Object targetValue, Class<?> type) {
    if (List.class.isAssignableFrom(type)) {
      return List.of(targetValue);
    }
    if (Set.class.isAssignableFrom(type)) {
      return Set.of(targetValue);
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
