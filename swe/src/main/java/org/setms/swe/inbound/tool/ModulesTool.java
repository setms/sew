package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.diagram.Shape.RECTANGLE;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.components;
import static org.setms.swe.inbound.tool.Inputs.domains;
import static org.setms.swe.inbound.tool.Inputs.modules;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.setms.km.domain.model.diagram.Arrow;
import org.setms.km.domain.model.diagram.BaseDiagramTool;
import org.setms.km.domain.model.diagram.Box;
import org.setms.km.domain.model.diagram.Diagram;
import org.setms.km.domain.model.diagram.Orientation;
import org.setms.km.domain.model.diagram.ShapeBox;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.architecture.Components;
import org.setms.swe.domain.model.sdlc.architecture.Module;
import org.setms.swe.domain.model.sdlc.architecture.Modules;
import org.setms.swe.domain.model.sdlc.ddd.Domain;
import org.setms.swe.domain.model.sdlc.ddd.Subdomain;
import org.setms.swe.domain.services.DeployModulesInComponents;

@Slf4j
public class ModulesTool extends BaseDiagramTool<Modules> {

  private static final String SUGGESTION_DEPLOY_IN_COMPONENTS = "modules.deploy.in.components";

  @Override
  public Input<Modules> getMainInput() {
    return modules();
  }

  @Override
  public Set<Input<?>> additionalInputs() {
    return Set.of(domains(), components());
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var domains = inputs.get(Domain.class);
    var components = inputs.get(Components.class);
    inputs
        .get(Modules.class)
        .forEach(
            modules -> {
              var location = modules.toLocation();
              validateByDomain(modules, domains, location, diagnostics);
              validateByComponents(modules, components, location, diagnostics);
            });
  }

  private void validateByDomain(
      Modules modules,
      List<Domain> domains,
      Location location,
      Collection<Diagnostic> diagnostics) {
    if (modules.getMappedTo() == null) {
      diagnostics.add(new Diagnostic(ERROR, "Must map to domain", location));
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
                          location)));
    }
  }

  private void validate(Modules modules, Domain domain, Collection<Diagnostic> diagnostics) {
    modules
        .getModules()
        .forEach(
            module -> {
              var location = modules.toLocation();
              if (module.getMappedTo() == null) {
                diagnostics.add(
                    new Diagnostic(ERROR, "Must map to subdomain", module.appendTo(location)));
              } else if (module.getMappedTo().resolveFrom(domain.getSubdomains()).isEmpty()) {
                diagnostics.add(
                    new Diagnostic(
                        ERROR,
                        "Unknown subdomain %s".formatted(module.getMappedTo().getId()),
                        module.appendTo(location)));
              }
            });
  }

  private void validateByComponents(
      Modules modules,
      List<Components> components,
      Location location,
      Collection<Diagnostic> diagnostics) {
    if (components.stream().map(Components::getDeploys).noneMatch(modules.linkTo()::equals)) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Modules aren't deployed in components",
              location,
              new Suggestion(SUGGESTION_DEPLOY_IN_COMPONENTS, "Deploy in components")));
    }
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> resource,
      Modules modules,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs)
      throws Exception {
    if (SUGGESTION_DEPLOY_IN_COMPONENTS.equals(suggestionCode)) {
      return deployInComponents(resource, modules);
    }
    return super.doApply(resource, modules, suggestionCode, location, inputs);
  }

  private AppliedSuggestion deployInComponents(Resource<?> modulesResource, Modules modules) {
    try {
      var components = new DeployModulesInComponents().apply(modules);
      var componentsResource = resourceFor(components, modules, modulesResource);
      try (var output = componentsResource.writeTo()) {
        builderFor(components).build(components, output);
      }
      return created(componentsResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }

  @Override
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var domains = inputs.get(Domain.class);
    inputs.get(Modules.class).forEach(modules -> build(modules, resource, diagnostics, domains));
  }

  private void build(
      Modules modules,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics,
      List<Domain> domains) {
    buildHtml(modules, null, toDiagram(modules, domains), resource, diagnostics);
  }

  private Diagram toDiagram(Modules modules, List<Domain> domains) {
    var result = new Diagram().setOrientation(Orientation.TOP_TO_BOTTOM);
    var boxes = new HashMap<Module, Box>();
    modules
        .getModules()
        .forEach(
            module -> boxes.put(module, result.add(new ShapeBox(module.getName(), RECTANGLE))));
    modules
        .getModules()
        .forEach(
            from ->
                dependenciesOf(from, modules, domains)
                    .forEach(to -> result.add(new Arrow(boxes.get(from), boxes.get(to)))));
    return result;
  }

  private Stream<Module> dependenciesOf(
      Module module, Modules modules, Collection<Domain> domains) {
    return modules.getMappedTo().resolveFrom(domains).stream()
        .flatMap(
            domain ->
                Stream.ofNullable(module.getMappedTo())
                    .map(link -> link.resolveFrom(domain.getSubdomains()))
                    .flatMap(Optional::stream)
                    .map(Subdomain::dependsOn)
                    .flatMap(Collection::stream)
                    .flatMap(
                        link ->
                            modules.getModules().stream()
                                .filter(m -> link.equals(m.getMappedTo()))));
  }
}
