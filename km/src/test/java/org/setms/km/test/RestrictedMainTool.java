package org.setms.km.test;

import java.util.Set;
import org.setms.km.domain.model.tool.GlobInput;
import org.setms.km.domain.model.tool.Input;

public class RestrictedMainTool extends TestTool<MainArtifact> {

  @Override
  public Set<Input<? extends MainArtifact>> validationTargets() {
    return Set.of(new GlobInput<>("restricted-main", TestFormat.INSTANCE, MainArtifact.class));
  }
}
