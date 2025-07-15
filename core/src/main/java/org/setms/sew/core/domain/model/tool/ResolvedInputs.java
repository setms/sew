package org.setms.sew.core.domain.model.tool;

import static org.setms.sew.core.domain.model.format.Strings.initLower;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.atteo.evo.inflector.English;
import org.setms.sew.core.domain.model.sdlc.*;

public class ResolvedInputs implements PointerResolver {

  private final Map<String, List<? extends NamedObject>> values = new HashMap<>();

  public void put(String name, List<? extends NamedObject> resolved) {
    values.put(name, resolved);
  }

  public <T extends NamedObject> List<T> get(Class<T> type) {
    return get(initLower(English.plural(type.getSimpleName())), type);
  }

  public <T extends NamedObject> List<T> get(String name, Class<T> type) {
    var namedObjects = Optional.ofNullable(values.get(name)).orElseGet(Collections::emptyList);
    return namedObjects.stream().map(type::cast).toList();
  }

  @Override
  public NamedObject resolve(Pointer pointer, String defaultType) {
    if (pointer == null) {
      return new UnresolvedObject(null, null);
    }
    var type = Optional.ofNullable(pointer.getType()).orElse(defaultType);
    if (type == null) {
      return new UnresolvedObject(new FullyQualifiedName(pointer.getId()), null);
    }
    var candidates = values.get(English.plural(type));
    var resolvedObject = pointer.resolveFrom(candidates);
    if (resolvedObject.isPresent()) {
      return resolvedObject.get();
    }
    return new UnresolvedObject(new FullyQualifiedName(pointer.getId()), pointer.getType());
  }
}
