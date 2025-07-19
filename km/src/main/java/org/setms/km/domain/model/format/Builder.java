package org.setms.km.domain.model.format;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.Pointer;
import org.setms.km.domain.model.tool.OutputSink;

public interface Builder {

  List<String> IGNORABLE_GETTERS =
      List.of("getClass", "getFullyQualifiedName", "getPackage", "getName");

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

  default void build(Artifact object, File file) throws IOException {
    build(toRootObject(object), file);
  }

  default void build(Artifact object, OutputSink sink) throws IOException {
    try (var output = sink.open()) {
      build(toRootObject(object), output);
    }
  }

  default RootObject toRootObject(Artifact object) {
    return setProperties(
        object,
        new RootObject(
            object.getPackage(),
            startLowerCase(object.getClass().getSimpleName()),
            object.getName()));
  }

  default <T extends DataObject<T>> T setProperties(Artifact source, T target) {
    Arrays.stream(source.getClass().getMethods())
        .filter(m -> m.getName().startsWith("get") || m.getName().startsWith("is"))
        .filter(m -> !IGNORABLE_GETTERS.contains(m.getName()))
        .forEach(m -> setProperty(source, m, target));
    return target;
  }

  default String startLowerCase(String value) {
    var result = new StringBuilder(value);
    result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
    return result.toString();
  }

  default void setProperty(Artifact source, Method getter, DataObject<?> target) {
    var getterName = getter.getName();
    try {
      var index = getterName.startsWith("get") ? 3 : 2;
      var name = startLowerCase(getterName.substring(index));
      var value = convert(getter.invoke(source));
      if (value != null) {
        target.set(name, value);
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to set property for " + getterName, e);
    }
  }

  @SuppressWarnings("rawtypes")
  default DataItem convert(Object value) {
    return switch (value) {
      case null -> null;
      case String string -> new DataString(string);
      case Boolean bool -> new DataEnum(Boolean.toString(bool));
      case Enum enumValue -> new DataEnum(enumValue.name().toLowerCase());
      case Collection<?> collection ->
          new DataList().add(collection.stream().map(this::convert).toList());
      case Pointer pointer -> {
        var attributes = new HashMap<String, List<Reference>>();
        pointer
            .getAttributes()
            .forEach(
                (key, pointers) ->
                    attributes.put(
                        key,
                        pointers.stream()
                            .map(ptr -> new Reference(ptr.getType(), ptr.getId()))
                            .toList()));
        yield new Reference(pointer.getType(), pointer.getId(), attributes);
      }
      case Artifact namedObject -> toNestedObject(namedObject);
      default ->
          throw new UnsupportedOperationException("Can't convert " + value.getClass().getName());
    };
  }

  default NestedObject toNestedObject(Artifact source) {
    return setProperties(source, new NestedObject(source.getName()));
  }

  default void build(Artifact object, OutputStream output) throws IOException {
    build(toRootObject(object), output);
  }
}
