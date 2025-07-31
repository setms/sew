package org.setms.km.test;

import java.util.*;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Output;

public class OtherTool extends TestTool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>("other", new TestFormat(), OtherArtifact.class),
        new Input<>("main", new TestFormat(), MainArtifact.class));
  }

  @Override
  public Optional<Output> getOutputs() {
    return Optional.empty();
  }

}
