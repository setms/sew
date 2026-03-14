package org.setms.swe.domain.model.sdlc.code.java.gradle;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

class BuildFile {

  private static final String NL = System.lineSeparator();
  private static final String DEPENDENCIES_START = "dependencies {" + NL;
  private static final String DEPENDENCIES_END = "}";

  private final StringBuilder content;

  BuildFile(String content) {
    this.content = new StringBuilder(content);
  }

  public void addDependency(String scope, String dependency) {
    var start = content.indexOf(DEPENDENCIES_START) + DEPENDENCIES_START.length();
    var end = content.indexOf(DEPENDENCIES_END, start);
    var dependencies = dependenciesFromString(content.substring(start, end));
    add(scope, dependency, dependencies);
    content.replace(start, end, dependenciesToString(dependencies));
  }

  private Map<String, Collection<String>> dependenciesFromString(String dependenciesBlock) {
    var result = new TreeMap<String, Collection<String>>();
    Arrays.stream(dependenciesBlock.trim().split("\n"))
        .map(String::trim)
        .forEach(
            line -> {
              var parts = line.split("\\s");
              if (parts.length >= 2) {
                add(parts[0], parts[1], result);
              }
            });
    return result;
  }

  private String dependenciesToString(Map<String, Collection<String>> dependencies) {
    return dependencies.entrySet().stream()
        .map(
            e ->
                e.getValue().stream()
                        .map(d -> "    %s %s".formatted(e.getKey(), d))
                        .collect(joining(NL))
                    + NL)
        .collect(joining(NL));
  }

  private void add(String scope, String dependency, Map<String, Collection<String>> dependencies) {
    dependencies.computeIfAbsent(scope, ignored -> new TreeSet<>()).add(dependency);
  }

  @Override
  public String toString() {
    return content.toString();
  }
}
