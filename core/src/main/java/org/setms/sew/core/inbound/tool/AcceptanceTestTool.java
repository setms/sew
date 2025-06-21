package org.setms.sew.core.inbound.tool;

import java.util.Collection;
import java.util.List;
import org.setms.sew.core.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.format.acceptance.AcceptanceFormat;

public class AcceptanceTestTool extends Tool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>(
            "acceptanceTests",
            new Glob("src/test/acceptance", "**/*.acceptance"),
            new AcceptanceFormat(),
            AcceptanceTest.class));
  }

  @Override
  public List<Output> getOutputs() {
    return List.of();
  }

  @Override
  protected void build(ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    // TODO: Implement
  }
}
