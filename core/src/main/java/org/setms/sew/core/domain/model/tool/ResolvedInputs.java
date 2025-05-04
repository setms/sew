package org.setms.sew.core.domain.model.tool;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.atteo.evo.inflector.English;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;

public class ResolvedInputs {

  private final Map<String, List<? extends NamedObject>> values = new HashMap<>();

  public void put(String name, List<? extends NamedObject> resolved) {
    values.put(name, resolved);
  }

  public <T extends NamedObject> List<T> get(String name, Class<T> type) {
    var namedObjects = Optional.ofNullable(values.get(name)).orElseGet(Collections::emptyList);
    return namedObjects.stream().map(type::cast).toList();
  }

  public NamedObject resolve(Pointer pointer) {
    var type = pointer.getType();
    if (type == null) {
      throw new IllegalArgumentException("Can't resolve untyped pointer");
    }
    var candidates = values.get(English.plural(type));
    var resolvedObject = pointer.resolveFrom(candidates);
    if (resolvedObject.isPresent()) {
      return resolvedObject.get();
    }
    return new UnresolvedObject(new FullyQualifiedName(pointer.getId()), pointer.getType());
  }
}
