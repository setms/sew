package org.setms.sew.core.inbound.format.sew;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.atteo.evo.inflector.English;
import org.setms.sew.core.domain.model.format.Builder;
import org.setms.sew.core.domain.model.format.DataItem;
import org.setms.sew.core.domain.model.format.DataList;
import org.setms.sew.core.domain.model.format.DataObject;
import org.setms.sew.core.domain.model.format.DataString;
import org.setms.sew.core.domain.model.format.NestedObject;
import org.setms.sew.core.domain.model.format.Reference;
import org.setms.sew.core.domain.model.format.RootObject;

class SewBuilder implements Builder {

  private static final String INDENT = "  ";

  @Override
  public void build(RootObject root, PrintWriter writer) {
    var nestedObjects = new LinkedHashMap<String, List<NestedObject>>();
    writer.format("package %s%n%n", root.getScope());
    buildObject(writer, root.getType(), root.getName(), root, nestedObjects);
    nestedObjects.forEach(
        (type, objects) ->
            objects.forEach(
                object -> {
                  writer.println();
                  buildObject(writer, singular(type), object.getName(), object, emptyMap());
                }));
  }

  private String singular(String type) {
    var result = type;
    if (result.endsWith("s")) {
      result = result.substring(0, result.length() - 1);
      if (!English.plural(result).equals(type) && result.endsWith("e")) {
        result = result.substring(0, result.length() - 1);
        if (!English.plural(result).equals(type)) {
          throw new IllegalStateException(
              "Don't know how to turn '%s' into singular form".formatted(type));
        }
      }
    }
    return result;
  }

  private void buildObject(
      PrintWriter writer,
      String type,
      String name,
      DataObject<?> object,
      Map<String, List<NestedObject>> nestedObjects) {
    writer.format("%s %s {%n", type, name);
    object.properties((key, value) -> buildProperty(writer, key, value, nestedObjects));
    writer.println("}");
  }

  private void buildProperty(
      PrintWriter writer,
      String key,
      DataItem value,
      Map<String, List<NestedObject>> nestedObjects) {
    if (value instanceof NestedObject nestedObject) {
      nestedObjects.put(key, List.of(nestedObject));
      return;
    }
    if (value instanceof DataList list
        && list.hasItems()
        && list.getFirst() instanceof NestedObject) {
      nestedObjects.put(key, list.map(NestedObject.class::cast).toList());
      return;
    }
    writer.format("%s%s = %s%n", INDENT, key, convert(value, INDENT));
  }

  private String convert(DataItem value, String indent) {
    return switch (value) {
      case DataString string -> convertString(string, indent);
      case DataList list -> convertList(list, indent);
      case Reference reference -> reference.getId();
      default ->
          throw new UnsupportedOperationException(
              "Can't build value of type " + value.getClass().getSimpleName());
    };
  }

  private String convertString(DataString string, String ignored) {
    return "\"%s\"".formatted(string.getValue());
  }

  private String convertList(DataList list, String indent) {
    var indented = indent + INDENT;
    return switch (list.size()) {
      case 0 -> "[ ]";
      case 1 -> "[ %s ]".formatted(convert(list.getFirst(), indent));
      default ->
          "[%n%s%s%n%s]"
              .formatted(
                  indented,
                  list.map(item -> convert(item, indented)).collect(joining(",\n" + indented)),
                  indent);
    };
  }
}
