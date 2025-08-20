package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.diagram.Shape.RECTANGLE;
import static org.setms.km.domain.model.validation.Level.ERROR;
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
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.architecture.Module;
import org.setms.swe.domain.model.sdlc.architecture.Modules;
import org.setms.swe.domain.model.sdlc.ddd.Domain;
import org.setms.swe.domain.model.sdlc.ddd.Subdomain;

@Slf4j
public class ModulesTool extends BaseDiagramTool<Modules> {

  @Override
  public Input<Modules> getMainInput() {
    return modules();
  }

  @Override
  public Set<Input<?>> additionalInputs() {
    return Set.of(domains());
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var domains = inputs.get(Domain.class);
    inputs
        .get(Modules.class)
        .forEach(
            modules -> {
              var location = modules.toLocation();
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
            });
  }

  private void validate(Modules modules, Domain domain, Collection<Diagnostic> diagnostics) {
    modules
        .getModules()
        .forEach(
            module -> {
              if (module.getMappedTo() == null) {
                Location location = modules.toLocation();
                diagnostics.add(
                    new Diagnostic(ERROR, "Must map to subdomain", module.appendTo(location)));
              } else if (module.getMappedTo().resolveFrom(domain.getSubdomains()).isEmpty()) {
                Location location = modules.toLocation();
                diagnostics.add(
                    new Diagnostic(
                        ERROR,
                        "Unknown subdomain %s".formatted(module.getMappedTo().getId()),
                        module.appendTo(location)));
              }
            });
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
