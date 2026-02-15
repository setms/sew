package org.setms.km.domain.model.tool;

import java.util.Set;

public class BarTool extends ArtifactTool<Bar> {

  @Override
  public Set<Input<? extends Bar>> validationTargets() {
    return Set.of(new GlobInput<>("bar", null, Bar.class));
  }
}
