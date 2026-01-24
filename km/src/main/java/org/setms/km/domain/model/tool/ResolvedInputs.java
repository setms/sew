package org.setms.km.domain.model.tool;

import static org.setms.km.domain.model.format.Strings.initLower;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.*;
import org.setms.km.domain.model.nlp.English;
import org.setms.km.domain.model.nlp.NaturalLanguage;

public class ResolvedInputs implements LinkResolver {

  private final NaturalLanguage language = new English();
  private final Map<String, List<Artifact>> values = new HashMap<>();

  public ResolvedInputs put(String name, List<? extends Artifact> resolved) {
    values.computeIfAbsent(name, _ -> new ArrayList<>()).addAll(resolved);
    return this;
  }

  public <T extends Artifact> List<T> get(Class<T> type) {
    return get(initLower(language.plural(type.getSimpleName())), type);
  }

  private <T extends Artifact> List<T> get(String name, Class<T> type) {
    var namedObjects = Optional.ofNullable(values.get(name)).orElseGet(Collections::emptyList);
    return namedObjects.stream().map(type::cast).toList();
  }

  @Override
  public Artifact resolve(Link link, String defaultType) {
    if (link == null) {
      return new UnresolvedArtifact(null, null);
    }
    var type = Optional.ofNullable(link.getType()).orElse(defaultType);
    if (type == null) {
      return new UnresolvedArtifact(new FullyQualifiedName(link.getId()), null);
    }
    var candidates = values.get(language.plural(type));
    var resolvedObject = link.resolveFrom(candidates);
    return resolvedObject.orElseGet(
        () -> new UnresolvedArtifact(new FullyQualifiedName(link.getId()), link.getType()));
  }

  public <A extends Artifact> Optional<A> find(Class<A> type, FullyQualifiedName name) {
    return Artifact.find(get(type), name);
  }

  public Stream<? extends Artifact> all() {
    return values.values().stream().flatMap(Collection::stream).distinct();
  }
}
