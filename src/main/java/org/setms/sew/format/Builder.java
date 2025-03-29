package org.setms.sew.format;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.setms.sew.schema.NamedObject;
import org.setms.sew.schema.Pointer;

public interface Builder {

  List<String> IGNORABLE_GETTERS = List.of("getClass", "getPackage", "getName");

  default void build(RootObject root, File file) throws IOException {
    try (var writer = new PrintWriter(file)) {
      build(root, writer);
    }
  }

  void build(RootObject root, PrintWriter writer) throws IOException;

  default void build(RootObject root, OutputStream output) throws IOException {
    try (var writer = new PrintWriter(output)) {
      build(root, writer);
    }
  }

  default void build(NamedObject object, File file) throws IOException {
    build(toRootObject(object), file);
  }

  default RootObject toRootObject(NamedObject object) {
    return setProperties(
        object,
        new RootObject(
            object.getPackage(),
            startLowerCase(object.getClass().getSimpleName()),
            object.getName()));
  }

  default <T extends DataObject<T>> T setProperties(NamedObject source, T target) {
    Arrays.stream(source.getClass().getMethods())
        .filter(m -> m.getName().startsWith("get"))
        .filter(m -> !IGNORABLE_GETTERS.contains(m.getName()))
        .forEach(m -> setProperty(source, m, target));
    return target;
  }

  default String startLowerCase(String value) {
    var result = new StringBuilder(value);
    result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
    return result.toString();
  }

  default void setProperty(NamedObject source, Method getter, DataObject<?> target) {
    try {
      var name = startLowerCase(getter.getName().substring(3));
      var value = convert(getter.invoke(source));
      if (value != null) {
        target.set(name, value);
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to set property for " + getter.getName(), e);
    }
  }

  default DataItem convert(Object value) {
    return switch (value) {
      case null -> null;
      case String string -> new DataString(string);
      case Collection<?> collection ->
          new DataList().add(collection.stream().map(this::convert).toList());
      case Pointer pointer -> new Reference(pointer.getId());
      case NamedObject namedObject -> toNestedObject(namedObject);
      default ->
          throw new UnsupportedOperationException("Can't convert " + value.getClass().getName());
    };
  }

  default NestedObject toNestedObject(NamedObject source) {
    return setProperties(source, new NestedObject(source.getName()));
  }

  default void build(NamedObject object, OutputStream output) throws IOException {
    build(toRootObject(object), output);
  }
}
