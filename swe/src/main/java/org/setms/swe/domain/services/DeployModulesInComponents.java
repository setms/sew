package org.setms.swe.domain.services;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.setms.swe.domain.model.sdlc.architecture.Component;
import org.setms.swe.domain.model.sdlc.architecture.Components;
import org.setms.swe.domain.model.sdlc.architecture.Module;
import org.setms.swe.domain.model.sdlc.architecture.Modules;

public class DeployModulesInComponents implements Function<Modules, Components> {

  @Override
  public Components apply(Modules modules) {
    return new Components(modules.getFullyQualifiedName())
        .setDeploys(modules.linkTo())
        .setComponents(divideOverComponents(modules));
  }

  private @NotEmpty Collection<@Valid Component> divideOverComponents(Modules modules) {
    // TODO: Shouldn't always be a monolith
    return List.of(
        new Component(modules.getFullyQualifiedName())
            .setDeploys(modules.getModules().stream().map(Module::linkTo).toList()));
  }
}
