package org.setms.sew.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.setms.sew.schema.FullyQualifiedName;
import org.setms.sew.schema.SchemaObject;

public interface Parser {

  default <T extends SchemaObject> T parse(InputStream input, Class<T> type) throws IOException {
    return convert(parse(input), type);
  }

  RootObject parse(InputStream input) throws IOException;

  default <T extends SchemaObject> T convert(RootObject rootObject, Class<T> type) {
    if (!rootObject.getType().equals(type.getSimpleName())) {
      throw new IllegalArgumentException(
          "Can't parse %s from %s".formatted(type.getName(), rootObject.getType()));
    }
    try {
      var result =
          type.getConstructor(FullyQualifiedName.class)
              .newInstance(
                  new FullyQualifiedName(
                      "%s.%s".formatted(rootObject.getScope(), rootObject.getName())));
      setProperties(rootObject, result);
      return result;
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException(e);
    }
  }

  default void setProperties(RootObject source, Object target) {
    source.properties(
        (name, value) -> {
          var targetValue =
              switch (value) {
                case DataString string -> string.getValue();
                default ->
                    throw new UnsupportedOperationException(
                        "Unexpected value of type " + value.getClass().getSimpleName());
              };
          setProperty(name, targetValue, target);
        });
  }

  default void setProperty(String name, Object targetValue, Object target) {
    var setter = "set%s%s".formatted(Character.toUpperCase(name.charAt(0)), name.substring(1));
    try {
      var method = target.getClass().getMethod(setter, targetValue.getClass());
      method.invoke(target, targetValue);
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException(
          "Failed to set %s.%s".formatted(target.getClass(), name), e);
    }
  }

  default <T extends SchemaObject> T parse(File file, Class<T> type) throws IOException {
    return convert(parse(file), type);
  }

  default RootObject parse(File file) throws IOException {
    try (var input = new FileInputStream(file)) {
      return parse(input);
    }
  }
}
