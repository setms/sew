package org.setms.sew.core.format.sew;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Optional;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.setms.sew.core.format.DataItem;
import org.setms.sew.core.format.DataList;
import org.setms.sew.core.format.DataObject;
import org.setms.sew.core.format.DataString;
import org.setms.sew.core.format.NestedObject;
import org.setms.sew.core.format.Parser;
import org.setms.sew.core.format.Reference;
import org.setms.sew.core.format.RootObject;

class SewParser implements Parser {

  @Override
  public RootObject parse(InputStream input) throws IOException {
    var sew = parseTreeFrom(input).sew();
    var result = parseRootObject(sew);
    parseProperties(sew.object(0), result);
    var nestedObjects = new LinkedHashMap<String, DataList>();
    sew.object().stream()
        .skip(1)
        .forEach(
            object -> {
              var type = object.TYPE().getText();
              var objectsOfType = nestedObjects.computeIfAbsent(type, ignored -> new DataList());
              var nestedObject = new NestedObject(object.OBJECT_NAME().getText());
              objectsOfType.add(nestedObject);
              parseProperties(object, nestedObject);
            });
    nestedObjects.forEach(
        (key, value) -> result.set(key, value.size() == 1 ? value.getFirst() : value));
    return result;
  }

  private org.setms.sew.antlr.SewParser parseTreeFrom(InputStream input) throws IOException {
    return new org.setms.sew.antlr.SewParser(
        new CommonTokenStream(
            (new org.setms.sew.antlr.SewLexer(CharStreams.fromStream(input, UTF_8)))));
  }

  private RootObject parseRootObject(org.setms.sew.antlr.SewParser.SewContext sew) {
    var scope =
        sew.scope().qualifiedName().IDENTIFIER().stream()
            .map(ParseTree::getText)
            .collect(joining("."));
    var rootObject = sew.object(0);
    return new RootObject(scope, rootObject.TYPE().getText(), rootObject.OBJECT_NAME().getText());
  }

  private void parseProperties(
      org.setms.sew.antlr.SewParser.ObjectContext object, DataObject<?> dataObject) {
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

  private DataItem parseList(org.setms.sew.antlr.SewParser.ListContext list) {
    var result = new DataList();
    list.item().stream().map(this::parseItem).forEach(result::add);
    return result;
  }

  private DataItem parseItem(org.setms.sew.antlr.SewParser.ItemContext item) {
    if (item.STRING() != null) {
      return toStringItem(item.STRING());
    }
    if (item.OBJECT_NAME() != null) {
      return new Reference(item.OBJECT_NAME().getText());
    }
    return new Reference(
        item.typedName().TYPE().getText(), item.typedName().OBJECT_NAME().getText());
  }

  private DataItem toStringItem(TerminalNode string) {
    return new DataString(stripQuotesFrom(string.getText()));
  }

  private String stripQuotesFrom(String quotedText) {
    return quotedText.substring(1, quotedText.length() - 1);
  }
}
