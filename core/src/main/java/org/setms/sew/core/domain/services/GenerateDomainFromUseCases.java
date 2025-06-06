package org.setms.sew.core.domain.services;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.setms.sew.core.domain.model.format.Strings.initUpper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.setms.sew.core.domain.model.dsm.Cluster;
import org.setms.sew.core.domain.model.dsm.ClusteringAlgorithm;
import org.setms.sew.core.domain.model.dsm.Dependency;
import org.setms.sew.core.domain.model.dsm.DesignStructureMatrix;
import org.setms.sew.core.domain.model.dsm.StochasticGradientDescentClusteringAlgorithm;
import org.setms.sew.core.domain.model.sdlc.Domain;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.UseCase;

@RequiredArgsConstructor
public class GenerateDomainFromUseCases implements Function<Collection<UseCase>, Domain> {

  private static final String AGGREGATE = "aggregate";
  private static final String POLICY = "policy";
  private static final String READ_MODEL = "readModel";
  private static final String EVENT = "event";
  private static final String COMMAND = "command";
  private static final String EXTERNAL_SYSTEM = "externalSystem";
  private static final String ATTR_UPDATES = "updates";
  private static final String ATTR_READS = "reads";
  private static final List<String> ACTIVE_ELEMENT_TYPES = List.of(AGGREGATE, READ_MODEL, POLICY);
  private static final int AVAILABILITY_COUPLING = 10;
  private static final int ANTI_CORRUPTION_COUPLING = 8;
  private static final int DATA_COUPLING = 4;
  private static final int CONTRACT_COUPLING = 1;

  private final ClusteringAlgorithm<Pointer> clusteringAlgorithm;

  public GenerateDomainFromUseCases() {
    this(new StochasticGradientDescentClusteringAlgorithm<>());
  }

  @Override
  public Domain apply(Collection<UseCase> useCases) {
    if (useCases.isEmpty()) {
      throw new IllegalArgumentException("Missing use cases");
    }
    var dsm = dsmFrom(useCases);
    var clusters = findClustersIn(dsm);
    return domainFrom(useCases, clusters, packageFrom(useCases));
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
    addDependenciesBetweenPolicies(model, dsm);
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
        .limit(steps.size() - 1)
        .filter(isType(types[0]))
        .map(fromStep -> toSequence(steps, fromStep, types))
        .filter(Objects::nonNull)
        .distinct();
  }

  private Predicate<Pointer> isType(String type) {
    return step -> step.isType(type);
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

  private void addDependenciesBetweenPoliciesAndReadModels(
      EventStormingModel model, DesignStructureMatrix<Pointer> dsm) {
    model
        .findSequences(READ_MODEL, POLICY)
        .forEach(
            sequence ->
                dsm.addDependency(sequence.last(), sequence.first(), AVAILABILITY_COUPLING));
  }

  private void addDependenciesBetweenReadModelsAndAggregates(
      EventStormingModel model, DesignStructureMatrix<Pointer> dsm) {
    model
        .findSequences(AGGREGATE, EVENT, READ_MODEL)
        .forEach(sequence -> dsm.addDependency(sequence.last(), sequence.first(), DATA_COUPLING));
  }

  private void addDependenciesBetweenPolicies(
      EventStormingModel model, DesignStructureMatrix<Pointer> dsm) {
    model
        .findSequences(POLICY, COMMAND, EXTERNAL_SYSTEM, EVENT, POLICY)
        .forEach(
            sequence -> {
              dsm.addDependency(sequence.first(), sequence.last(), ANTI_CORRUPTION_COUPLING);
              dsm.addDependency(sequence.last(), sequence.first(), ANTI_CORRUPTION_COUPLING);
            });
  }

  private Set<Cluster<Pointer>> findClustersIn(DesignStructureMatrix<Pointer> dsm) {
    var result = clusteringAlgorithm.apply(dsm);
    var dependencies = dsm.getDependencies();
    result.forEach(
        cluster ->
            cluster.stream()
                .flatMap(element -> dependenciesOf(element, dependencies))
                .map(dependency -> clusterOf(dependency, result))
                .filter(not(cluster::equals))
                .forEach(cluster::addDependency));
    return result;
  }

  private Stream<Pointer> dependenciesOf(
      Pointer element, Collection<Dependency<Pointer>> dependencies) {
    return dependencies.stream().filter(d -> d.from().equals(element)).map(Dependency::to);
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

  private Domain domainFrom(
      Collection<UseCase> useCases, Set<Cluster<Pointer>> clusters, String packageName) {
    return new Domain(toFullyQualifiedName(packageName))
        .setSubdomains(subdomainsFor(packageName, useCases, clusters));
  }

  private FullyQualifiedName toFullyQualifiedName(String packageName) {
    return new FullyQualifiedName("%s.%s".formatted(packageName, initUpper(packageName)));
  }

  private List<Domain.Subdomain> subdomainsFor(
      String packageName, Collection<UseCase> useCases, Set<Cluster<Pointer>> clusters) {
    var contracts = new TreeSet<Pointer>();
    var clustersBySubdomain = new HashMap<Cluster<Pointer>, Domain.Subdomain>();
    var result =
        clusters.stream()
            .map(
                cluster -> {
                  var domain = toDomain(useCases, packageName, cluster);
                  clustersBySubdomain.put(cluster, domain);
                  return domain;
                })
            .collect(toList());
    addDependencies(clustersBySubdomain);
    result = simplify(result);
    addEvents(useCases, clusters, contracts);
    if (!contracts.isEmpty()) {
      if (result.size() == 1) {
        result.getFirst().getContent().addAll(contracts);
      } else {
        result.add(toContractsDomain(packageName, contracts));
      }
    }
    return result;
  }

  private Domain.Subdomain toDomain(
      Collection<UseCase> useCases, String packageName, Cluster<Pointer> currentCluster) {
    addCommands(useCases, currentCluster);
    return new Domain.Subdomain(
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
        .distinct()
        .forEach(event -> addEvent(event, useCases, clusters, contractsCluster));
  }

  private void addEvent(
      Pointer event,
      Collection<UseCase> useCases,
      Set<Cluster<Pointer>> clusters,
      Set<Pointer> contractsCluster) {
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
        .filter(sequence -> event.equals(sequence.last()))
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
        .filter(sequence -> event.equals(sequence.first()))
        .map(Sequence::last)
        .map(handler -> clusterOf(handler, allClusters))
        .filter(not(result::contains))
        .forEach(result::add);
    return result;
  }

  private Domain.Subdomain toContractsDomain(String packageName, Set<Pointer> content) {
    return new Domain.Subdomain(new FullyQualifiedName("%s.Contracts".formatted(packageName)))
        .setContent(content);
  }

  private String nameFor(Cluster<Pointer> cluster) {
    var result =
        cluster.stream().filter(isType(AGGREGATE)).map(Pointer::getId).collect(joining("And"));
    if (result.isEmpty()) {
      result = cluster.stream().filter(isType(POLICY)).map(Pointer::getId).collect(joining("And"));
    }
    if (result.isEmpty()) {
      result = cluster.stream().map(Pointer::getId).collect(joining("And"));
    }
    return result;
  }

  private void addDependencies(Map<Cluster<Pointer>, Domain.Subdomain> clustersBySubdomain) {
    clustersBySubdomain.forEach(
        ((cluster, domain) -> {
          var dependencies =
              cluster.getDependencies().stream()
                  .map(clustersBySubdomain::get)
                  .map(bc -> new Pointer("domain", bc.getName()))
                  .collect(toSet());
          if (!dependencies.isEmpty()) {
            domain.setDependsOn(dependencies);
          }
        }));
  }

  private List<Domain.Subdomain> simplify(List<Domain.Subdomain> subdomains) {
    var candidatesForMerging =
        subdomains.stream()
            .filter(
                subdomain -> !subdomain.dependsOn().isEmpty() && subdomain.dependsOn().size() <= 2)
            .filter(
                subdomain -> subdomain.getContent().stream().noneMatch(c -> c.isType(AGGREGATE)))
            .toList();
    candidatesForMerging.forEach(
        subdomain -> {
          if (subdomain.dependsOn().size() == 1) {
            var dependsOn =
                subdomain.dependsOn().iterator().next().resolveFrom(subdomains).orElseThrow();
            merge(subdomain, dependsOn, subdomains);
          } else {
            var depends =
                subdomain.dependsOn().stream()
                    .map(p -> p.resolveFrom(subdomains).orElseThrow())
                    .toList();
            var d1 = depends.getFirst();
            var d2 = depends.getLast();
            if (dependsOn(d1, d2, subdomains)) {
              merge(subdomain, d2, subdomains);
            } else if (dependsOn(d2, d1, subdomains)) {
              merge(subdomain, d1, subdomains);
            }
          }
        });
    Collections.sort(subdomains);
    return subdomains;
  }

  private void merge(
      Domain.Subdomain subdomain, Domain.Subdomain into, List<Domain.Subdomain> subdomains) {
    into.getContent().addAll(subdomain.getContent());
    subdomains.remove(subdomain);
  }

  private boolean dependsOn(
      Domain.Subdomain subdomain, Domain.Subdomain candidate, List<Domain.Subdomain> subdomains) {
    return subdomain.dependsOn().stream()
        .map(p -> p.resolveFrom(subdomains).orElseThrow())
        .anyMatch(candidate::equals);
  }
}
