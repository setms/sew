package org.setms.sew.core.domain.services;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.setms.sew.core.domain.model.format.Strings.initUpper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.setms.sew.core.domain.model.dsm.Cluster;
import org.setms.sew.core.domain.model.dsm.ClusteringAlgorithm;
import org.setms.sew.core.domain.model.dsm.DesignStructureMatrix;
import org.setms.sew.core.domain.model.dsm.StochasticGradientDescentClusteringAlgorithm;
import org.setms.sew.core.domain.model.sdlc.ContextMap;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.UseCase;

@RequiredArgsConstructor
public class GenerateContextMapFromUseCases implements Function<Collection<UseCase>, ContextMap> {

  private static final String AGGREGATE = "aggregate";
  private static final String POLICY = "policy";
  private static final String READ_MODEL = "readModel";
  private static final String EVENT = "event";
  private static final String COMMAND = "command";
  private static final String ATTR_UPDATES = "updates";
  private static final String ATTR_READS = "reads";
  private static final List<String> ACTIVE_ELEMENT_TYPES = List.of(AGGREGATE, READ_MODEL, POLICY);
  private static final int AVAILABILITY_COUPLING = 10;
  private static final int DATA_COUPLING = 4;
  private static final int CONTRACT_COUPLING = 1;

  private final ClusteringAlgorithm<Pointer> clusteringAlgorithm;

  public GenerateContextMapFromUseCases() {
    this(new StochasticGradientDescentClusteringAlgorithm<>());
  }

  @Override
  public ContextMap apply(Collection<UseCase> useCases) {
    if (useCases.isEmpty()) {
      throw new IllegalArgumentException("Missing use cases");
    }
    var dsm = dsmFrom(useCases);
    var clusters = findClustersIn(dsm);
    clusters.forEach(cluster -> System.out.printf("- cluster: %s%n", cluster));
    var result = contextMapFrom(useCases, clusters, packageFrom(useCases));
    result
        .getBoundedContexts()
        .forEach(
            context -> System.out.printf("- %s: %s%n", context.getName(), context.getContent()));
    return result;
  }

  private Set<Cluster<Pointer>> findClustersIn(DesignStructureMatrix<Pointer> dsm) {
    return clusteringAlgorithm.apply(dsm);
  }

  private DesignStructureMatrix<Pointer> dsmFrom(Collection<UseCase> useCases) {
    var completeEventStorm = combine(useCases);
    var result = new DesignStructureMatrix<>(activeElementsIn(completeEventStorm));
    addDependencies(completeEventStorm, result);
    return result;
  }

  private EventStormingModel combine(Collection<UseCase> useCases) {
    var result = new EventStormingModel();
    useCases.stream()
        .flatMap(UseCase::scenarios)
        .map(UseCase.Scenario::getSteps)
        .forEach(
            steps -> {
              for (var i = 0; i < steps.size() - 1; i++) {
                addStep(steps.get(i), result);
                result.add(steps.get(i), steps.get(i + 1));
              }
              addStep(steps.getLast(), result);
            });
    return result;
  }

  private void addStep(Pointer step, EventStormingModel model) {
    step.optAttribute(ATTR_READS).forEach(readModel -> model.add(readModel, step));
    step.optAttribute(ATTR_UPDATES).forEach(readModel -> model.add(step, readModel));
  }

  private Set<Pointer> activeElementsIn(EventStormingModel model) {
    return model.elements().stream()
        .filter(p -> ACTIVE_ELEMENT_TYPES.contains(p.getType()))
        .sorted(this::compareActiveElements)
        .collect(toCollection(LinkedHashSet::new));
  }

  private int compareActiveElements(Pointer p1, Pointer p2) {
    var result =
        ACTIVE_ELEMENT_TYPES.indexOf(p1.getType()) - ACTIVE_ELEMENT_TYPES.indexOf(p2.getType());
    if (result != 0) {
      return result;
    }
    return p1.getId().compareTo(p2.getId());
  }

  private void addDependencies(EventStormingModel model, DesignStructureMatrix<Pointer> dsm) {
    addDependenciesBetweenPoliciesAndAggregates(model, dsm);
    addDependenciesBetweenPoliciesAndReadModels(model, dsm);
    addDependenciesBetweenReadModelsAndAggregates(model, dsm);
  }

  private void addDependenciesBetweenPoliciesAndAggregates(
      EventStormingModel model, DesignStructureMatrix<Pointer> dsm) {
    model
        .findSequences(POLICY, COMMAND, AGGREGATE)
        .forEach(
            sequence -> dsm.addDependency(sequence.first(), sequence.last(), CONTRACT_COUPLING));
    model
        .findSequences(AGGREGATE, EVENT, POLICY)
        .forEach(
            sequence -> dsm.addDependency(sequence.last(), sequence.first(), CONTRACT_COUPLING));
  }

  private Stream<Sequence> findSequences(List<Pointer> steps, String... types) {
    if (steps.size() < types.length) {
      return Stream.empty();
    }
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

  private void addDependenciesBetweenReadModelsAndAggregates(
      EventStormingModel model, DesignStructureMatrix<Pointer> dsm) {
    model
        .findSequences(AGGREGATE, EVENT, READ_MODEL)
        .forEach(sequence -> dsm.addDependency(sequence.last(), sequence.first(), DATA_COUPLING));
  }

  private void addDependenciesBetweenPoliciesAndReadModels(
      EventStormingModel model, DesignStructureMatrix<Pointer> dsm) {
    model
        .findSequences(READ_MODEL, POLICY)
        .forEach(
            sequence ->
                dsm.addDependency(sequence.last(), sequence.first(), AVAILABILITY_COUPLING));
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
    event.optAttribute(ATTR_UPDATES).stream()
        .map(readModel -> clusterOf(readModel, allClusters))
        .forEach(result::add);
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
    var result =
        cluster.stream().filter(isType(AGGREGATE)).map(Pointer::getId).collect(joining("And"));
    if (result.isEmpty()) {
      return cluster.stream().map(Pointer::getId).collect(joining("And"));
    }
    return result;
  }
}
