package org.setms.sew.core.inbound.tool;

import static org.setms.sew.core.domain.model.tool.Level.ERROR;

import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.setms.sew.core.domain.model.sdlc.Domain;
import org.setms.sew.core.domain.model.sdlc.Modules;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Location;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Tool;

@Slf4j
public class ModulesTool extends Tool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>("src/main/architecture", Modules.class),
        new Input<>("src/main/requirements", Domain.class));
  }

  @Override
  public List<Output> getOutputs() {
    return List.of();
  }

  @Override
  protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var domains = inputs.get(Domain.class);
    inputs
        .get(Modules.class)
        .forEach(
            modules -> {
              if (modules.getMappedTo() == null) {
                diagnostics.add(
                    new Diagnostic(
                        ERROR, "Must map to domain", new Location("modules", modules.getName())));
              } else {
                modules
                    .getMappedTo()
                    .resolveFrom(domains)
                    .ifPresentOrElse(
                        domain -> validate(modules, domain, diagnostics),
                        () ->
                            diagnostics.add(
                                new Diagnostic(
                                    ERROR,
                                    "Unknown domain %s".formatted(modules.getMappedTo().getId()),
                                    new Location("modules", modules.getName()))));
              }
            });
  }

  private void validate(Modules modules, Domain domain, Collection<Diagnostic> diagnostics) {
    modules
        .getModules()
        .forEach(
            module -> {
              if (module.getMappedTo() == null) {
                diagnostics.add(
                    new Diagnostic(
                        ERROR,
                        "Must map to subdomain",
                        new Location("modules", modules.getName(), "module", module.getName())));
              } else if (module.getMappedTo().resolveFrom(domain.getSubdomains()).isEmpty()) {
                diagnostics.add(
                    new Diagnostic(
                        ERROR,
                        "Unknown subdomain %s".formatted(module.getMappedTo().getId()),
                        new Location("modules", modules.getName(), "module", module.getName())));
              }
            });
  }
}
