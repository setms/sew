package org.setms.sew.core.inbound.format.sal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.setms.sew.core.domain.model.format.Strings.initUpper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.setms.sew.core.domain.model.format.DataEnum;
import org.setms.sew.core.domain.model.format.DataItem;
import org.setms.sew.core.domain.model.format.DataList;
import org.setms.sew.core.domain.model.format.DataObject;
import org.setms.sew.core.domain.model.format.DataString;
import org.setms.sew.core.domain.model.format.NestedObject;
import org.setms.sew.core.domain.model.format.Parser;
import org.setms.sew.core.domain.model.format.Reference;
import org.setms.sew.core.domain.model.format.RootObject;
import org.setms.sew.core.domain.model.format.Strings;
import org.setms.sew.lang.sal.SalLexer;
import org.setms.sew.lang.sal.SalParser;

class SalFormatParser implements Parser {

  @Override
  public RootObject parse(InputStream input) throws IOException {
    var sal = parseTreeFrom(input).sal();
    var result = parseRootObject(sal);
    if (result == null) {
      return null;
    }
    parseProperties(sal.object(0), result);
    var nestedObjects = new LinkedHashMap<String, DataList>();
    var index = new AtomicInteger(0);
    sal.object().stream()
        .skip(1)
        .filter(object -> object.TYPE() != null)
        .forEach(
            object -> {
              var type = object.TYPE().getText();
              var objectsOfType = nestedObjects.computeIfAbsent(type, ignored -> new DataList());
              index.getAndIncrement();
              var name =
                  Optional.ofNullable(object.OBJECT_NAME())
                      .map(TerminalNode::getText)
                      .orElseGet(() -> initUpper(type) + index.get());
              var nestedObject = new NestedObject(name);
              objectsOfType.add(nestedObject);
              parseProperties(object, nestedObject);
            });
    nestedObjects.forEach(
        (key, value) -> result.set(key, value.size() == 1 ? value.getFirst() : value));
    return result;
  }

  private SalParser parseTreeFrom(InputStream input) throws IOException {
    return new SalParser(
        new CommonTokenStream((new SalLexer(CharStreams.fromStream(input, UTF_8)))));
  }

  private RootObject parseRootObject(SalParser.SalContext sal) {
    if (sal.scope() == null
        || sal.scope().qualifiedName() == null
        || sal.scope().qualifiedName().IDENTIFIER() == null
        || sal.object() == null
        || sal.object().isEmpty()
        || sal.object(0).TYPE() == null
        || sal.object(0).OBJECT_NAME() == null) {
      return null;
    }
    var scope =
        sal.scope().qualifiedName().IDENTIFIER().stream()
            .map(ParseTree::getText)
            .collect(joining("."));
    var rootObject = sal.object(0);
    return new RootObject(scope, rootObject.TYPE().getText(), rootObject.OBJECT_NAME().getText());
  }

  private void parseProperties(SalParser.ObjectContext object, DataObject<?> dataObject) {
    object
        .property()
        .forEach(
            property -> {
              var key = property.IDENTIFIER().getText();
              var value =
                  Optional.ofNullable(property.list())
                      .map(this::parseList)
                      .orElseGet(() -> parseItem(property.item()));
              dataObject.set(key, value);
            });
  }

  private DataItem parseList(SalParser.ListContext list) {
    var result = new DataList();
    list.item().stream().map(this::parseItem).filter(Objects::nonNull).forEach(result::add);
    return result;
  }

  private DataItem parseItem(SalParser.ItemContext item) {
    if (item == null) {
      return null;
    }
    if (item.STRING() != null) {
      return toStringItem(item.STRING());
    }
    if (item.IDENTIFIER() != null) {
      return new DataEnum(item.IDENTIFIER().getText());
    }
    if (item.OBJECT_NAME() != null) {
      return new Reference(item.OBJECT_NAME().getText());
    }
    if (item.typedReference() == null
        || item.typedReference().OBJECT_NAME() == null
        || item.typedReference().TYPE() == null) {
      return null;
    }
    Map<String, List<Reference>> map;
    var attributes = item.typedReference().attribute();
    if (attributes == null) {
      map = Collections.emptyMap();
    } else {
      map = new HashMap<>();
      attributes.forEach(
          attribute -> {
            var value = attribute.attributeValue();
            if (attribute.IDENTIFIER() != null
                && value != null
                && value.TYPE() != null
                && value.OBJECT_NAME() != null) {
              map.computeIfAbsent(attribute.IDENTIFIER().getText(), ignored -> new ArrayList<>())
                  .add(new Reference(value.TYPE().getText(), value.OBJECT_NAME().getText()));
            }
          });
    }
    return new Reference(
        item.typedReference().TYPE().getText(), item.typedReference().OBJECT_NAME().getText(), map);
  }

  private DataItem toStringItem(TerminalNode string) {
    return new DataString(Strings.stripQuotesFrom(string.getText()));
  }
}
