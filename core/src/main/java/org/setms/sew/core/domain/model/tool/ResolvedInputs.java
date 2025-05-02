package org.setms.sew.core.domain.model.tool;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

public class ResolvedInputs {

  private final Map<String, List<? extends NamedObject>> values = new HashMap<>();

  public void put(String name, List<? extends NamedObject> resolved) {
    values.put(name, resolved);
  }

  public <T extends NamedObject> List<T> get(String name, Class<T> type) {
    var namedObjects = Optional.ofNullable(values.get(name)).orElseGet(Collections::emptyList);
    return namedObjects.stream().map(type::cast).toList();
  }
}
