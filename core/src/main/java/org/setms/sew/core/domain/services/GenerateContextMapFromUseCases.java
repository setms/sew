package org.setms.sew.core.domain.services;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.setms.sew.core.domain.model.format.Strings.initUpper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.setms.sew.core.domain.model.dsm.Cluster;
import org.setms.sew.core.domain.model.dsm.DesignStructureMatrix;
import org.setms.sew.core.domain.model.dsm.TotalCostMinimizingClusteringAlgorithm;
import org.setms.sew.core.domain.model.sdlc.ContextMap;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.UseCase;

public class GenerateContextMapFromUseCases implements Function<Collection<UseCase>, ContextMap> {

  private static final String AGGREGATE = "aggregate";
  private static final String POLICY = "policy";
  private static final String READ_MODEL = "readModel";
  private static final String EVENT = "event";
  private static final String COMMAND = "command";
  private static final String ATTR_UPDATES = "updates";
  private static final String ATTR_READS = "reads";
  private static final Collection<String> ACTIVE_ELEMENT_TYPES =
      List.of(AGGREGATE, POLICY, READ_MODEL);
  private static final Collection<String> ACTIVE_ELEMENT_CONTAINER_TYPES = List.of(EVENT);
  private static final int AVAILABILITY_COUPLING_STRENGTH = 5;
  private static final int CONTRACT_COUPLING_STRENGTH = 3;

  @Override
  public ContextMap apply(Collection<UseCase> useCases) {
    if (useCases.isEmpty()) {
      throw new IllegalArgumentException("Missing use cases");
    }
    var dsm = dsmFrom(useCases);
    var clusters = new TotalCostMinimizingClusteringAlgorithm<>(dsm).findClusters();
    return contextMapFrom(useCases, clusters, packageFrom(useCases));
  }

  private DesignStructureMatrix<Pointer> dsmFrom(Collection<UseCase> useCases) {
    var result = new DesignStructureMatrix<>(activeElementsFrom(useCases));
    addDependencies(useCases, result);
    return result;
  }

  private Set<Pointer> activeElementsFrom(Collection<UseCase> useCases) {
    return useCases.stream()
        .flatMap(UseCase::scenarios)
        .flatMap(UseCase.Scenario::steps)
        .filter(this::isPotentiallyActive)
        .flatMap(this::extractActiveElementsFrom)
        .filter(this::isActive)
        .collect(toSet());
  }

  private boolean isPotentiallyActive(Pointer pointer) {
    return isActive(pointer) || ACTIVE_ELEMENT_CONTAINER_TYPES.contains(pointer.getType());
  }

  private boolean isActive(Pointer pointer) {
    return ACTIVE_ELEMENT_TYPES.contains(pointer.getType());
  }

  private Stream<Pointer> extractActiveElementsFrom(Pointer element) {
    var result = new ArrayList<Pointer>();
    result.add(element);
    addReferenceFromAttribute(element, POLICY, ATTR_READS, result);
    addReferenceFromAttribute(element, EVENT, ATTR_UPDATES, result);
    return result.stream();
  }

  private void addReferenceFromAttribute(
      Pointer element, String type, String attribute, Collection<Pointer> references) {
    if (type.equals(element.getType())) {
      element.optAttribute(attribute).ifPresent(references::add);
    }
  }

  private void addDependencies(Collection<UseCase> useCases, DesignStructureMatrix<Pointer> dsm) {
    useCases.stream()
        .flatMap(UseCase::scenarios)
        .map(UseCase.Scenario::getSteps)
        .forEach(steps -> addDependencies(steps, dsm));
  }

  private void addDependencies(List<Pointer> steps, DesignStructureMatrix<Pointer> dsm) {
    addPoliciesDependingOnAggregates(steps, dsm);
    addPoliciesDependingOnReadModels(steps, dsm);
    addReadModelsDependingOnAggregates(steps, dsm);
  }

  private void addPoliciesDependingOnAggregates(
      List<Pointer> steps, DesignStructureMatrix<Pointer> dsm) {
    findSequences(steps, POLICY, COMMAND, AGGREGATE)
        .forEach(
            sequence ->
                dsm.addDependency(sequence.first(), sequence.last(), CONTRACT_COUPLING_STRENGTH));
    findSequences(steps, AGGREGATE, EVENT, POLICY)
        .forEach(
            sequence ->
                dsm.addDependency(sequence.last(), sequence.first(), CONTRACT_COUPLING_STRENGTH));
  }

  private Stream<Sequence> findSequences(List<Pointer> steps, String... types) {
    return steps.stream()
        .limit(steps.size() - 2)
        .filter(isType(types[0]))
        .map(fromStep -> toSequence(steps, fromStep, types))
        .filter(Objects::nonNull);
  }

  private Predicate<Pointer> isType(String type) {
    return step -> type.equals(step.getType());
  }

  private Sequence toSequence(List<Pointer> steps, Pointer fromStep, String[] types) {
    Pointer toStep = null;
    var index = steps.indexOf(fromStep);
    for (var i = 1; i < types.length; i++) {
      toStep = steps.get(++index);
      if (!types[i].equals(toStep.getType())) {
        return null;
      }
    }
    return new Sequence(fromStep, toStep);
  }

  private void addReadModelsDependingOnAggregates(
      List<Pointer> steps, DesignStructureMatrix<Pointer> dsm) {
    findSequences(steps, AGGREGATE, EVENT, READ_MODEL)
        .forEach(
            sequence ->
                dsm.addDependency(sequence.last(), sequence.first(), CONTRACT_COUPLING_STRENGTH));
    findSequences(steps, AGGREGATE, EVENT)
        .flatMap(this::replaceEventByReadModelThatItUpdates)
        .forEach(
            sequence ->
                dsm.addDependency(sequence.last(), sequence.first(), CONTRACT_COUPLING_STRENGTH));
  }

  private Stream<Sequence> replaceEventByReadModelThatItUpdates(Sequence sequence) {
    return sequence
        .last()
        .optAttribute(ATTR_UPDATES)
        .map(readModel -> new Sequence(sequence.first(), readModel))
        .stream();
  }

  private void addPoliciesDependingOnReadModels(
      List<Pointer> steps, DesignStructureMatrix<Pointer> dsm) {
    steps.stream()
        .filter(isType(POLICY))
        .map(
            policy ->
                policy.optAttribute(ATTR_READS).map(readModel -> new Sequence(readModel, policy)))
        .flatMap(Optional::stream)
        .forEach(
            sequence ->
                dsm.addDependency(
                    sequence.first(), sequence.last(), AVAILABILITY_COUPLING_STRENGTH));
  }

  private String packageFrom(Collection<UseCase> useCases) {
    var packages = useCases.stream().map(UseCase::getPackage).toList();
    if (packages.size() == 1) {
      return packages.getFirst();
    }
    var packagesBySize = packages.stream().collect(groupingBy(p -> p)).values();
    var mostCommon = packagesBySize.stream().mapToInt(List::size).max().orElseThrow();
    return packagesBySize.stream()
        .filter(v -> v.size() == mostCommon)
        .findFirst()
        .orElseThrow()
        .getFirst();
  }

  private ContextMap contextMapFrom(
      Collection<UseCase> useCases, Set<Cluster<Pointer>> clusters, String packageName) {
    return new ContextMap(toFullyQualifiedName(packageName))
        .setBoundedContexts(boundedContextsFor(packageName, useCases, clusters));
  }

  private FullyQualifiedName toFullyQualifiedName(String packageName) {
    return new FullyQualifiedName("%s.%s".formatted(packageName, initUpper(packageName)));
  }

  private List<ContextMap.BoundedContext> boundedContextsFor(
      String packageName, Collection<UseCase> useCases, Set<Cluster<Pointer>> clusters) {
    var contracts = new TreeSet<Pointer>();
    var result =
        clusters.stream()
            .map(cluster -> toBoundedContext(useCases, packageName, cluster))
            .collect(toList());
    addEvents(useCases, clusters, contracts);
    if (!contracts.isEmpty()) {
      result.add(toContractsBoundedContext(packageName, contracts));
    }
    return result;
  }

  private ContextMap.BoundedContext toBoundedContext(
      Collection<UseCase> useCases, String packageName, Cluster<Pointer> currentCluster) {
    addCommands(useCases, currentCluster);
    return new ContextMap.BoundedContext(
            new FullyQualifiedName("%s.%s".formatted(packageName, nameFor(currentCluster))))
        .setContent(currentCluster);
  }

  private void addCommands(Collection<UseCase> useCases, Collection<Pointer> elements) {
    useCases.stream()
        .map(UseCase::getScenarios)
        .flatMap(Collection::stream)
        .map(UseCase.Scenario::getSteps)
        .flatMap(steps -> findSequences(steps, COMMAND, AGGREGATE))
        .filter(sequence -> elements.contains(sequence.last()))
        .map(Sequence::first)
        .forEach(elements::add);
  }

  private void addEvents(
      Collection<UseCase> useCases, Set<Cluster<Pointer>> clusters, Set<Pointer> contractsCluster) {
    useCases.stream()
        .flatMap(UseCase::scenarios)
        .flatMap(UseCase.Scenario::steps)
        .filter(isType(EVENT))
        .forEach(event -> addEvent(useCases, clusters, contractsCluster, event));
  }

  private void addEvent(
      Collection<UseCase> useCases,
      Set<Cluster<Pointer>> clusters,
      Set<Pointer> contractsCluster,
      Pointer event) {
    var emitting = clustersEmitting(event, useCases, clusters);
    if (emitting.size() == 1) {
      emitting.getFirst().add(event);
      return;
    }
    var handling = clustersHandling(event, useCases, clusters);
    if (handling.size() == 1) {
      handling.getFirst().add(event);
      return;
    }
    contractsCluster.add(event);
  }

  private List<Cluster<Pointer>> clustersEmitting(
      Pointer event, Collection<UseCase> useCases, Set<Cluster<Pointer>> allClusters) {
    return useCases.stream()
        .flatMap(UseCase::scenarios)
        .map(UseCase.Scenario::getSteps)
        .flatMap(steps -> findSequences(steps, AGGREGATE, EVENT))
        .filter(sequence -> event == sequence.last())
        .map(Sequence::first)
        .map(aggregate -> clusterOf(aggregate, allClusters))
        .distinct()
        .toList();
  }

  private Cluster<Pointer> clusterOf(Pointer element, Set<Cluster<Pointer>> candidates) {
    return candidates.stream()
        .filter(cluster -> cluster.contains(element))
        .findFirst()
        .orElseThrow();
  }

  private List<Cluster<Pointer>> clustersHandling(
      Pointer event, Collection<UseCase> useCases, Set<Cluster<Pointer>> allClusters) {
    var result = new ArrayList<Cluster<Pointer>>();
    event
        .optAttribute(ATTR_UPDATES)
        .map(readModel -> clusterOf(readModel, allClusters))
        .ifPresent(result::add);
    useCases.stream()
        .flatMap(UseCase::scenarios)
        .map(UseCase.Scenario::getSteps)
        .flatMap(
            steps ->
                Stream.concat(
                    findSequences(steps, EVENT, POLICY), findSequences(steps, EVENT, READ_MODEL)))
        .filter(sequence -> event == sequence.first())
        .map(Sequence::last)
        .map(handler -> clusterOf(handler, allClusters))
        .filter(not(result::contains))
        .forEach(result::add);
    return result;
  }

  private ContextMap.BoundedContext toContractsBoundedContext(
      String packageName, Set<Pointer> content) {
    return new ContextMap.BoundedContext(
            new FullyQualifiedName("%s.Contracts".formatted(packageName)))
        .setContent(content);
  }

  private String nameFor(Cluster<Pointer> cluster) {
    return cluster.stream().filter(isType(AGGREGATE)).map(Pointer::getId).collect(joining("And"));
  }

  private record Sequence(Pointer first, Pointer last) {}
}
