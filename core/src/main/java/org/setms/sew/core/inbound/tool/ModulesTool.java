package org.setms.sew.core.inbound.tool;

import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.setms.sew.core.domain.model.sdlc.Modules;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Tool;

@Slf4j
public class ModulesTool extends Tool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(new Input<>("src/main/architecture", Modules.class));
  }

  @Override
  public List<Output> getOutputs() {
    return List.of();
  }

  @Override
  protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    inputs
        .get(Modules.class)
        .forEach(
            modules -> {
              log.info("{}", modules);
            });
  }
}
