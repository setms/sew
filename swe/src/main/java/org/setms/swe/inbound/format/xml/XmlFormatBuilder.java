package org.setms.swe.inbound.format.xml;

import static java.util.stream.Collectors.joining;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.DataEnum;
import org.setms.km.domain.model.format.DataList;
import org.setms.km.domain.model.format.DataObject;
import org.setms.km.domain.model.format.DataString;
import org.setms.km.domain.model.format.NestedObject;
import org.setms.km.domain.model.format.RootObject;
import org.setms.km.domain.model.nlp.English;
import org.setms.km.domain.model.nlp.NaturalLanguage;

class XmlFormatBuilder implements Builder {

  private final NaturalLanguage language = new English();

  @Override
  public void build(RootObject root, PrintWriter writer) {
    writeElement(writer, root.getType(), root.getScope(), root.getName(), root, 0);
    writer.flush();
  }

  private void writeElement(
      PrintWriter writer, String tag, String scope, String name, DataObject<?> obj, int depth) {
    var indent = "  ".repeat(depth);
    var attributes = new LinkedHashMap<String, String>();
    var childLists = new LinkedHashMap<String, DataList>();
    if (scope != null) attributes.put("package", scope);
    if (name != null && !name.isEmpty()) attributes.put("name", name);
    obj.properties(
        (key, value) -> {
          switch (value) {
            case DataEnum e -> attributes.put(key, e.getName());
            case DataString s -> attributes.put(key, s.getValue());
            case DataList list -> childLists.put(key, list);
            default -> {}
          }
        });
    var attrString =
        attributes.entrySet().stream()
            .map(e -> " %s=\"%s\"".formatted(e.getKey(), e.getValue()))
            .collect(joining());
    if (childLists.isEmpty()) {
      writer.println("%s<%s%s/>".formatted(indent, tag, attrString));
    } else {
      writer.println("%s<%s%s>".formatted(indent, tag, attrString));
      childLists.forEach(
          (key, list) ->
              list.map(NestedObject.class::cast)
                  .forEach(
                      item ->
                          writeElement(
                              writer,
                              language.singular(key),
                              null,
                              item.getName(),
                              item,
                              depth + 1)));
      writer.println("%s</%s>".formatted(indent, tag));
    }
  }
}
