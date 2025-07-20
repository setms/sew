package org.setms.sew.core.domain.services;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.setms.km.domain.model.format.Strings.initUpper;

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
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.sew.core.domain.model.dsm.Cluster;
import org.setms.sew.core.domain.model.dsm.ClusteringAlgorithm;
import org.setms.sew.core.domain.model.dsm.Dependency;
import org.setms.sew.core.domain.model.dsm.DesignStructureMatrix;
import org.setms.sew.core.domain.model.dsm.StochasticGradientDescentClusteringAlgorithm;
import org.setms.sew.core.domain.model.sdlc.ddd.Domain;
import org.setms.sew.core.domain.model.sdlc.ddd.EventStorm;
import org.setms.sew.core.domain.model.sdlc.ddd.Sequence;
import org.setms.sew.core.domain.model.sdlc.ddd.Subdomain;
import org.setms.sew.core.domain.model.sdlc.usecase.Scenario;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;

@RequiredArgsConstructor
public class DiscoverDomainFromUseCases implements Function<Collection<UseCase>, Domain> {

  private static final String AGGREGATE = "aggregate";
  private static final String POLICY = "policy";
  private static final String READ_MODEL = "readModel";
  private static final String EVENT = "event";
  private static final String COMMAND = "command";
  private static final String EXTERNAL_SYSTEM = "externalSystem";
  private static final String ATTR_UPDATES = "updates";
  private static final List<String> ACTIVE_ELEMENT_TYPES = List.of(AGGREGATE, READ_MODEL, POLICY);
  private static final int AVAILABILITY_COUPLING = 10;
  private static final int ANTI_CORRUPTION_COUPLING = 8;
  private static final int DATA_COUPLING = 4;
  private static final int CONTRACT_COUPLING = 1;

  private final ClusteringAlgorithm<Link> clusteringAlgorithm;

  public DiscoverDomainFromUseCases() {
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

  private DesignStructureMatrix<Link> dsmFrom(Collection<UseCase> useCases) {
    var completeEventStorm = new EventStorm(useCases);
    var result = new DesignStructureMatrix<>(activeElementsIn(completeEventStorm));
    addDependencies(completeEventStorm, result);
    return result;
  }

  private Set<Link> activeElementsIn(EventStorm model) {
    return model.elements().stream()
        .filter(p -> ACTIVE_ELEMENT_TYPES.contains(p.getType()))
        .sorted(this::compareActiveElements)
        .collect(toCollection(LinkedHashSet::new));
  }

  private int compareActiveElements(Link p1, Link p2) {
    var result =
        ACTIVE_ELEMENT_TYPES.indexOf(p1.getType()) - ACTIVE_ELEMENT_TYPES.indexOf(p2.getType());
    if (result != 0) {
      return result;
    }
    return p1.getId().compareTo(p2.getId());
  }

  private void addDependencies(EventStorm model, DesignStructureMatrix<Link> dsm) {
    addDependenciesBetweenPoliciesAndAggregates(model, dsm);
    addDependenciesBetweenPoliciesAndReadModels(model, dsm);
    addDependenciesBetweenReadModelsAndAggregates(model, dsm);
    addDependenciesBetweenPolicies(model, dsm);
  }

  private void addDependenciesBetweenPoliciesAndAggregates(
      EventStorm model, DesignStructureMatrix<Link> dsm) {
    model
        .findSequences(POLICY, COMMAND, AGGREGATE)
        .forEach(
            sequence -> dsm.addDependency(sequence.first(), sequence.last(), CONTRACT_COUPLING));
    model
        .findSequences(AGGREGATE, EVENT, POLICY)
        .forEach(
            sequence -> dsm.addDependency(sequence.last(), sequence.first(), CONTRACT_COUPLING));
  }

  private Stream<Sequence> findSequences(List<Link> steps, String... types) {
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

  private Predicate<Link> isType(String type) {
    return step -> step.hasType(type);
  }

  private Sequence toSequence(List<Link> steps, Link fromStep, String[] types) {
    Link toStep = null;
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
      EventStorm model, DesignStructureMatrix<Link> dsm) {
    model
        .findSequences(READ_MODEL, POLICY)
        .forEach(
            sequence ->
                dsm.addDependency(sequence.last(), sequence.first(), AVAILABILITY_COUPLING));
  }

  private void addDependenciesBetweenReadModelsAndAggregates(
      EventStorm model, DesignStructureMatrix<Link> dsm) {
    model
        .findSequences(AGGREGATE, EVENT, READ_MODEL)
        .forEach(sequence -> dsm.addDependency(sequence.last(), sequence.first(), DATA_COUPLING));
  }

  private void addDependenciesBetweenPolicies(EventStorm model, DesignStructureMatrix<Link> dsm) {
    model
        .findSequences(POLICY, COMMAND, EXTERNAL_SYSTEM, EVENT, POLICY)
        .forEach(
            sequence -> {
              dsm.addDependency(sequence.first(), sequence.last(), ANTI_CORRUPTION_COUPLING);
              dsm.addDependency(sequence.last(), sequence.first(), ANTI_CORRUPTION_COUPLING);
            });
  }

  private Set<Cluster<Link>> findClustersIn(DesignStructureMatrix<Link> dsm) {
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

  private Stream<Link> dependenciesOf(Link element, Collection<Dependency<Link>> dependencies) {
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
      Collection<UseCase> useCases, Set<Cluster<Link>> clusters, String packageName) {
    return new Domain(toFullyQualifiedName(packageName))
        .setSubdomains(subdomainsFor(packageName, useCases, clusters));
  }

  private FullyQualifiedName toFullyQualifiedName(String packageName) {
    return new FullyQualifiedName("%s.%s".formatted(packageName, initUpper(packageName)));
  }

  private List<Subdomain> subdomainsFor(
      String packageName, Collection<UseCase> useCases, Set<Cluster<Link>> clusters) {
    var contracts = new TreeSet<Link>();
    var clustersBySubdomain = new HashMap<Cluster<Link>, Subdomain>();
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

  private Subdomain toDomain(
      Collection<UseCase> useCases, String packageName, Cluster<Link> currentCluster) {
    addCommands(useCases, currentCluster);
    return new Subdomain(
            new FullyQualifiedName("%s.%s".formatted(packageName, nameFor(currentCluster))))
        .setContent(currentCluster);
  }

  private void addCommands(Collection<UseCase> useCases, Collection<Link> elements) {
    useCases.stream()
        .map(UseCase::getScenarios)
        .flatMap(Collection::stream)
        .map(Scenario::getSteps)
        .flatMap(steps -> findSequences(steps, COMMAND, AGGREGATE))
        .filter(sequence -> elements.contains(sequence.last()))
        .map(Sequence::first)
        .forEach(elements::add);
  }

  private void addEvents(
      Collection<UseCase> useCases, Set<Cluster<Link>> clusters, Set<Link> contractsCluster) {
    useCases.stream()
        .flatMap(UseCase::scenarios)
        .flatMap(Scenario::steps)
        .filter(isType(EVENT))
        .distinct()
        .forEach(event -> addEvent(event, useCases, clusters, contractsCluster));
  }

  private void addEvent(
      Link event,
      Collection<UseCase> useCases,
      Set<Cluster<Link>> clusters,
      Set<Link> contractsCluster) {
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

  private List<Cluster<Link>> clustersEmitting(
      Link event, Collection<UseCase> useCases, Set<Cluster<Link>> allClusters) {
    return useCases.stream()
        .flatMap(UseCase::scenarios)
        .map(Scenario::getSteps)
        .flatMap(steps -> findSequences(steps, AGGREGATE, EVENT))
        .filter(sequence -> event.equals(sequence.last()))
        .map(Sequence::first)
        .map(aggregate -> clusterOf(aggregate, allClusters))
        .distinct()
        .toList();
  }

  private Cluster<Link> clusterOf(Link element, Set<Cluster<Link>> candidates) {
    return candidates.stream()
        .filter(cluster -> cluster.contains(element))
        .findFirst()
        .orElseThrow();
  }

  private List<Cluster<Link>> clustersHandling(
      Link event, Collection<UseCase> useCases, Set<Cluster<Link>> allClusters) {
    var result = new ArrayList<Cluster<Link>>();
    event.optAttribute(ATTR_UPDATES).stream()
        .map(readModel -> clusterOf(readModel, allClusters))
        .forEach(result::add);
    useCases.stream()
        .flatMap(UseCase::scenarios)
        .map(Scenario::getSteps)
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

  private Subdomain toContractsDomain(String packageName, Set<Link> content) {
    return new Subdomain(new FullyQualifiedName("%s.Contracts".formatted(packageName)))
        .setContent(content);
  }

  private String nameFor(Cluster<Link> cluster) {
    var result =
        cluster.stream().filter(isType(AGGREGATE)).map(Link::getId).collect(joining("And"));
    if (result.isEmpty()) {
      result = cluster.stream().filter(isType(POLICY)).map(Link::getId).collect(joining("And"));
    }
    if (result.isEmpty()) {
      result = cluster.stream().map(Link::getId).collect(joining("And"));
    }
    return result;
  }

  private void addDependencies(Map<Cluster<Link>, Subdomain> clustersBySubdomain) {
    clustersBySubdomain.forEach(
        ((cluster, domain) -> {
          var dependencies =
              cluster.getDependencies().stream()
                  .map(clustersBySubdomain::get)
                  .map(Subdomain::linkTo)
                  .collect(toSet());
          if (!dependencies.isEmpty()) {
            domain.setDependsOn(dependencies);
          }
        }));
  }

  private List<Subdomain> simplify(List<Subdomain> subdomains) {
    var candidatesForMerging =
        subdomains.stream()
            .filter(
                subdomain -> !subdomain.dependsOn().isEmpty() && subdomain.dependsOn().size() <= 2)
            .filter(
                subdomain -> subdomain.getContent().stream().noneMatch(c -> c.hasType(AGGREGATE)))
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

  private void merge(Subdomain subdomain, Subdomain into, List<Subdomain> subdomains) {
    into.getContent().addAll(subdomain.getContent());
    subdomains.remove(subdomain);
  }

  private boolean dependsOn(Subdomain subdomain, Subdomain candidate, List<Subdomain> subdomains) {
    return subdomain.dependsOn().stream()
        .map(p -> p.resolveFrom(subdomains).orElseThrow())
        .anyMatch(candidate::equals);
  }
}
