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
              var type = object.type().getText();
              var objectsOfType = nestedObjects.computeIfAbsent(type, ignored -> new DataList());
              var nestedObject = new NestedObject(object.name().getText());
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
        sew.scope().qualifiedName().name().stream().map(ParseTree::getText).collect(joining("."));
    var rootObject = sew.object(0);
    return new RootObject(scope, rootObject.type().getText(), rootObject.name().getText());
  }

  private void parseProperties(
      org.setms.sew.antlr.SewParser.ObjectContext object, DataObject<?> dataObject) {
    object
        .property()
        .forEach(
            property -> {
              var key = property.name().getText();
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
    return Optional.ofNullable(item.reference())
        .map(this::toReferenceItem)
        .orElseGet(() -> toStringItem(item.string()));
  }

  private DataItem toReferenceItem(org.setms.sew.antlr.SewParser.ReferenceContext reference) {
    return new Reference(reference.getText());
  }

  private DataItem toStringItem(org.setms.sew.antlr.SewParser.StringContext string) {
    return new DataString(stripQuotesFrom(string.getText()));
  }

  private String stripQuotesFrom(String quotedText) {
    return quotedText.substring(1, quotedText.length() - 1);
  }
}
