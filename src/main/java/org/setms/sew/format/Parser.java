package org.setms.sew.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.setms.sew.schema.FullyQualifiedName;
import org.setms.sew.schema.NamedObject;
import org.setms.sew.schema.Pointer;

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
    return parseNamedObject(object, type, object.getScope(), object.getName());
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
      case Reference reference -> new Pointer(reference.getId());
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
            .filter(c -> name.equalsIgnoreCase(c.getSimpleName()))
            .map(c -> (Class<NamedObject>) c)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Can't find class " + name));
    return parseNamedObject(source, type, name, source.getName());
  }

  default void setProperty(String name, Object targetValue, Object target) {
    var setter = "set%s%s".formatted(Character.toUpperCase(name.charAt(0)), name.substring(1));
    try {
      var method = findSetter(setter, targetValue, target.getClass());
      if (method != null) {
        method.invoke(target, targetValue);
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException(
          "Failed to set %s.%s".formatted(target.getClass(), name), e);
    }
  }

  default Method findSetter(String name, Object targetValue, Class<?> type) {
    var methods = Arrays.stream(type.getMethods()).filter(m -> name.equals(m.getName())).toList();
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

  default <T extends NamedObject> T parse(File file, Class<T> type) throws IOException {
    return convert(parse(file), type);
  }

  default RootObject parse(File file) throws IOException {
    try (var input = new FileInputStream(file)) {
      return parse(input);
    }
  }
}
