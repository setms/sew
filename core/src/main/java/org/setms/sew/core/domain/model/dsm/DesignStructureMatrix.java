package org.setms.sew.core.domain.model.dsm;

import static java.lang.Math.max;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import org.atteo.evo.inflector.English;

public class DesignStructureMatrix<E> {

  private static final double DEFAULT_WEIGHT = 1.0;
  private static final String SEPARATOR = "|";
  private static final String NL = System.lineSeparator();

  @Getter private final List<E> elements = new ArrayList<>();
  private final Map<E, Map<E, Double>> interactionWeights = new HashMap<>();

  @SafeVarargs
  public DesignStructureMatrix(E... elements) {
    this(Arrays.stream(elements).collect(toCollection(LinkedHashSet::new)));
  }

  public DesignStructureMatrix(Set<E> elements) {
    if (elements == null || elements.size() < 2) {
      throw new IllegalArgumentException("DSM must contain at least two elements");
    }
    this.elements.addAll(elements);
  }

  @SuppressWarnings("UnusedReturnValue")
  public DesignStructureMatrix<E> addDependency(E from, E to) {
    return addDependency(from, to, DEFAULT_WEIGHT);
  }

  public DesignStructureMatrix<E> addDependency(E from, E to, double weight) {
    if (!elements.contains(from) || !elements.contains(to)) {
      throw new IllegalArgumentException("Unknown element");
    }
    var dependency = interactionWeights.computeIfAbsent(from, ignored -> new HashMap<>());
    var current = dependency.getOrDefault(to, 0.0);
    dependency.put(to, Math.max(current, weight));
    return this;
  }

  public Optional<Double> getWeight(E from, E to) {
    return Optional.ofNullable(interactionWeights.get(from))
        .map(dependencies -> dependencies.get(to));
  }

  public Collection<Dependency<E>> getDependencies() {
    var result = new LinkedHashSet<Dependency<E>>();
    interactionWeights.forEach(
        (from, dependents) ->
            dependents.forEach((to, weight) -> result.add(new Dependency<>(from, to, weight))));
    return result;
  }

  public DesignStructureMatrix<E> without(Collection<E> toRemove) {
    if (toRemove.isEmpty()) {
      return this;
    }
    var unknown = toRemove.stream().filter(not(elements::contains)).map(Objects::toString).toList();
    if (!unknown.isEmpty()) {
      throw new IllegalArgumentException(
          "Unknown %s can't be removed from DSM: %s"
              .formatted(English.plural("element", unknown.size()), String.join(", ", unknown)));
    }
    var newElements = new LinkedHashSet<>(this.elements);
    toRemove.forEach(newElements::remove);
    var result = new DesignStructureMatrix<>(newElements);
    interactionWeights.forEach(
        (from, weightsByTarget) -> {
          if (!toRemove.contains(from)) {
            weightsByTarget.forEach(
                (to, weight) -> {
                  if (!toRemove.contains(to)) {
                    result.addDependency(from, to, weight);
                  }
                });
          }
        });
    return result;
  }

  @Override
  public String toString() {
    var result = new StringBuilder();
    var maxLength =
        elements.stream().map(Object::toString).mapToInt(String::length).max().orElseThrow();
    appendHeader(maxLength, result);
    appendLine(elements.size(), maxLength, result);
    var uniqueWeights =
        interactionWeights.values().stream()
            .map(Map::values)
            .flatMap(Collection::stream)
            .distinct()
            .toList();
    var unweighted =
        uniqueWeights.isEmpty()
            || (uniqueWeights.size() == 1 && uniqueWeights.getFirst() == DEFAULT_WEIGHT);
    for (int i = 0; i < elements.size(); i++) {
      appendRow(i, maxLength, unweighted, result);
    }
    return result.toString();
  }

  private void appendHeader(int cellLength, StringBuilder text) {
    text.append(SEPARATOR).append(" ".repeat(cellLength + 2)).append(SEPARATOR);
    elements.stream()
        .map(Object::toString)
        .map(value -> padded(value, cellLength))
        .forEach(e -> text.append(' ').append(e).append(' ').append(SEPARATOR));
    text.append(NL);
  }

  private String padded(String value, int length) {
    var result = new StringBuilder();
    var missing = (length - value.length()) / 2;
    result.append(" ".repeat(max(0, missing)));
    result.append(value);
    result.append(" ".repeat(max(0, length - value.length() - missing)));
    return result.toString();
  }

  private void appendLine(int numCells, int cellWidth, StringBuilder text) {
    for (int i = 0; i <= numCells; i++) {
      text.append(SEPARATOR).append("-".repeat(cellWidth + 2));
    }
    text.append(SEPARATOR).append(NL);
  }

  private void appendRow(int index, int maxLength, boolean unweighted, StringBuilder text) {
    var element = elements.get(index);
    text.append(SEPARATOR).append(padded(element.toString(), maxLength + 2)).append(SEPARATOR);
    for (var i = 0; i < elements.size(); i++) {
      var other = elements.get(i);
      var weight = Optional.ofNullable(interactionWeights.get(element)).map(m -> m.get(other));
      var symbol =
          i == index ? "X" : weight.map(value -> unweighted ? "X" : value.toString()).orElse(" ");
      text.append(' ').append(padded(symbol, maxLength)).append(' ').append(SEPARATOR);
    }
    text.append(NL);
  }

  public record Dependency<E>(E from, E to, double weight) {}
}
