package org.setms.km.outbound.diagram.jgraphx;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class Css {

  private static final String PROPERTY_SEPARATOR = ";";
  private static final String NAME_VALUE_SEPARATOR = "=";

  private final Map<String, String> properties = new TreeMap<>();

  public Css(String style) {
    Optional.ofNullable(style).map(s -> s.split(PROPERTY_SEPARATOR)).stream()
        .flatMap(Arrays::stream)
        .map(nameValue -> nameValue.split(NAME_VALUE_SEPARATOR))
        .filter(a -> a.length == 2)
        .forEach(a -> properties.put(a[0], a[1]));
  }

  public Css set(String name, String value) {
    properties.put(name, value);
    return this;
  }

  public String toString() {
    return properties.entrySet().stream()
        .map(e -> "%s%s%s".formatted(e.getKey(), NAME_VALUE_SEPARATOR, e.getValue()))
        .collect(joining(PROPERTY_SEPARATOR));
  }
}
