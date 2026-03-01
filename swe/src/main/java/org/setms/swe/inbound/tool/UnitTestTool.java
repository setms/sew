package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.initiatives;

import java.util.HashSet;
import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;

public class UnitTestTool extends ArtifactTool<UnitTest> {

  @Override
  public Set<Input<? extends UnitTest>> validationTargets() {
    var result = new HashSet<Input<? extends UnitTest>>();
    result.addAll(Inputs.unitTests());
    return result;
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return Set.of(decisions(), initiatives());
  }
}
