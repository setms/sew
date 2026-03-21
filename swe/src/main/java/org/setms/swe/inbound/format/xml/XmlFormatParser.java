package org.setms.swe.inbound.format.xml;

import static java.util.stream.Collectors.groupingBy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.setms.km.domain.model.format.DataEnum;
import org.setms.km.domain.model.format.DataItem;
import org.setms.km.domain.model.format.DataList;
import org.setms.km.domain.model.format.DataObject;
import org.setms.km.domain.model.format.NestedObject;
import org.setms.km.domain.model.format.Parser;
import org.setms.km.domain.model.format.RootObject;
import org.setms.km.domain.model.nlp.English;
import org.setms.km.domain.model.nlp.NaturalLanguage;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class XmlFormatParser implements Parser {

  private final NaturalLanguage language = new English();

  @Override
  public RootObject parse(InputStream input) throws IOException {
    try {
      var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
      var root = doc.getDocumentElement();
      var result =
          new RootObject(
              root.getAttribute("package"), root.getTagName(), root.getAttribute("name"));
      parseProperties(root, result);
      return result;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Failed to parse XML", e);
    }
  }

  private void parseProperties(Element element, DataObject<?> target) {
    parseAttributes(element, target);
    parseChildren(element, target);
  }

  private void parseAttributes(Element element, DataObject<?> target) {
    var attrs = element.getAttributes();
    for (var i = 0; i < attrs.getLength(); i++) {
      var attr = attrs.item(i);
      var attrName = attr.getNodeName();
      if (!attrName.equals("package") && !attrName.equals("name")) {
        target.set(attrName, new DataEnum(attr.getNodeValue()));
      }
    }
  }

  private void parseChildren(Element element, DataObject<?> target) {
    var children = childElements(element).toList();
    parseWrappers(children, target);
    parseNamedChildren(children, target);
  }

  private void parseWrappers(List<Element> children, DataObject<?> target) {
    children.stream()
        .filter(e -> !e.hasAttribute("name"))
        .forEach(wrapper -> target.set(wrapper.getTagName(), parseWrapperContent(wrapper)));
  }

  private DataItem parseWrapperContent(Element wrapper) {
    var items = childElements(wrapper).map(this::parseNestedObject).toList();
    return toDataList(items);
  }

  private void parseNamedChildren(List<Element> children, DataObject<?> target) {
    children.stream()
        .filter(e -> e.hasAttribute("name"))
        .collect(groupingBy(Element::getTagName))
        .forEach(
            (tagName, elements) ->
                target.set(
                    language.plural(tagName),
                    toDataList(elements.stream().map(this::parseNestedObject).toList())));
  }

  private NestedObject parseNestedObject(Element element) {
    var result = new NestedObject(element.getAttribute("name")).setType(element.getTagName());
    parseProperties(element, result);
    return result;
  }

  private DataList toDataList(List<NestedObject> items) {
    var result = new DataList();
    items.forEach(result::add);
    return result;
  }

  private Stream<Element> childElements(Element element) {
    var result = new ArrayList<Element>();
    var children = element.getChildNodes();
    for (var i = 0; i < children.getLength(); i++) {
      var child = children.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        result.add((Element) child);
      }
    }
    return result.stream();
  }
}
