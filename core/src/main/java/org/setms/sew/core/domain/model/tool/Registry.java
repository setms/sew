package org.setms.sew.core.domain.model.tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Registry {

  private static final Collection<Tool> tools = new ArrayList<>();

  public static void register(Tool tool) {
    tools.add(tool);
  }

  public static Collection<Tool> getTools() {
    return Collections.unmodifiableCollection(tools);
  }
}
