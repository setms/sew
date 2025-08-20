package org.setms.swe.inbound.tool;

import static java.util.stream.Collectors.toSet;
import static org.setms.km.domain.model.diagram.Shape.ELLIPSE;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
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
import org.setms.swe.domain.model.sdlc.architecture.Module;
import org.setms.swe.domain.model.sdlc.architecture.Modules;
import org.setms.swe.domain.model.sdlc.ddd.Domain;
import org.setms.swe.domain.model.sdlc.ddd.Subdomain;

public class DomainTool extends BaseDiagramTool<Domain> {

  public static final String CREATE_MODULES = "modules.create";
  public static final int WIDTH = 120;
  public static final int HEIGHT = 60;

  @Override
  public Input<Domain> getMainInput() {
    return domains();
  }

  @Override
  public Set<Input<?>> additionalInputs() {
    return Set.of(useCases(), modules());
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var modules = inputs.get(Modules.class);
    inputs.get(Domain.class).forEach(domain -> validate(domain, modules, diagnostics));
  }

  private void validate(
      Domain domain, Collection<Modules> modules, Collection<Diagnostic> diagnostics) {
    if (modules.stream().noneMatch(containsModulesForAllSubdomainsIn(domain))) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Subdomains aren't mapped to modules",
              domain.toLocation(),
              List.of(new Suggestion(CREATE_MODULES, "Map to modules"))));
    }
  }

  private Predicate<? super Modules> containsModulesForAllSubdomainsIn(Domain domain) {
    return modules ->
        modules.getModules().stream()
            .map(Module::getMappedTo)
            .map(Link::getId)
            .collect(toSet())
            .equals(domain.getSubdomains().stream().map(Subdomain::getName).collect(toSet()));
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> domainResource,
      Domain domain,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs)
      throws IOException {
    if (CREATE_MODULES.equals(suggestionCode)) {
      return createModules(domainResource, domain);
    }
    return unknown(suggestionCode);
  }

  private AppliedSuggestion createModules(Resource<?> domainResource, Domain domain)
      throws IOException {
    var modules = mapDomainToModules(domain);
    var modulesResource = resourceFor(modules, domain, domainResource);
    try (var output = modulesResource.writeTo()) {
      builderFor(modules).build(modules, output);
      return created(modulesResource);
    }
  }

  private Modules mapDomainToModules(Domain domain) {
    var result = new Modules(getFullyQualifiedName(domain));
    result.setMappedTo(domain.linkTo());
    result.setModules(domain.getSubdomains().stream().map(this::subdmainToModule).toList());
    return result;
  }

  private FullyQualifiedName getFullyQualifiedName(Artifact object) {
    return new FullyQualifiedName("%s.%s".formatted(object.getPackage(), object.getName()));
  }

  private Module subdmainToModule(Subdomain subdomain) {
    var result = new Module(getFullyQualifiedName(subdomain));
    result.setMappedTo(subdomain.linkTo());
    return result;
  }

  @Override
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    inputs.get(Domain.class).forEach(domain -> build(domain, resource, diagnostics));
  }

  private void build(Domain domain, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    buildHtml(domain, null, toDiagram(domain), resource, diagnostics);
  }

  private Diagram toDiagram(Domain domain) {
    var result = new Diagram().setOrientation(Orientation.TOP_TO_BOTTOM);
    var boxes = new HashMap<Subdomain, Box>();
    domain.getSubdomains().forEach(subdomain -> boxes.put(subdomain, addBox(subdomain, result)));
    domain
        .getSubdomains()
        .forEach(
            source ->
                source.dependsOn().forEach(link -> addEdge(domain, source, link, boxes, result)));
    return result;
  }

  private Box addBox(Subdomain domain, Diagram diagram) {
    return diagram.add(new ShapeBox(domain.getName(), ELLIPSE, WIDTH, HEIGHT));
  }

  private void addEdge(
      Domain domain, Subdomain source, Link link, Map<Subdomain, Box> boxes, Diagram diagram) {
    link.resolveFrom(domain.getSubdomains())
        .ifPresent(target -> diagram.add(new Arrow(boxes.get(source), boxes.get(target))));
  }
}
