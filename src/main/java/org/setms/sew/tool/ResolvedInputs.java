package org.setms.sew.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.setms.sew.schema.NamedObject;

public class ResolvedInputs {

  private final Map<String, List<? extends NamedObject>> values = new HashMap<>();

  public void put(String name, List<? extends NamedObject> resolved) {
    values.put(name, resolved);
  }

  public <T extends NamedObject> List<T> get(String name, Class<T> type) {
    return values.get(name).stream().map(type::cast).toList();
  }
}
