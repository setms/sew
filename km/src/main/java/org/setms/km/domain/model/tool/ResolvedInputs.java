package org.setms.km.domain.model.tool;

import static org.setms.km.domain.model.format.Strings.initLower;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.setms.km.domain.model.artifact.*;
import org.setms.km.domain.model.nlp.English;
import org.setms.km.domain.model.nlp.NaturalLanguage;

public class ResolvedInputs implements LinkResolver {

  private final NaturalLanguage language = new English();
  private final Map<String, List<? extends Artifact>> values = new HashMap<>();

  public void put(String name, List<? extends Artifact> resolved) {
    values.put(name, resolved);
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
    if (resolvedObject.isPresent()) {
      return resolvedObject.get();
    }
    return new UnresolvedArtifact(new FullyQualifiedName(link.getId()), link.getType());
  }
}
