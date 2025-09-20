package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.diagram.Shape.ELLIPSE;
import static org.setms.swe.inbound.tool.Inputs.domains;

import java.util.*;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.diagram.Arrow;
import org.setms.km.domain.model.diagram.BaseDiagramTool;
import org.setms.km.domain.model.diagram.Box;
import org.setms.km.domain.model.diagram.Diagram;
import org.setms.km.domain.model.diagram.ShapeBox;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.ddd.Domain;
import org.setms.swe.domain.model.sdlc.ddd.Subdomain;

public class ContextMapTool extends BaseDiagramTool<Domain> {

  private static final String LABEL_UPSTREAM = "U";
  private static final String LABEL_DOWNSTREAM = "D";

  @Override
  public Input<Domain> getMainInput() {
    return null;
  }

  @Override
  public Set<Input<? extends Artifact>> additionalInputs() {
    return Set.of(domains());
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
    var result = new Diagram();
    var boxes = addBoxes(domain, result);
    addArrows(domain, boxes, result);
    return result;
  }

  private Map<Subdomain, Box> addBoxes(Domain domain, Diagram diagram) {
    var result = new HashMap<Subdomain, Box>();
    domain
        .getSubdomains()
        .forEach(
            subdomain ->
                result.put(subdomain, diagram.add(new ShapeBox(subdomain.getName(), ELLIPSE))));
    return result;
  }

  private void addArrows(Domain domain, Map<Subdomain, Box> boxes, Diagram diagram) {
    domain
        .getSubdomains()
        .forEach(
            downstream ->
                downstream.dependsOn().stream()
                    .map(link -> link.resolveFrom(domain.getSubdomains()))
                    .flatMap(Optional::stream)
                    .forEach(
                        upstream ->
                            diagram.add(
                                new Arrow(
                                    boxes.get(upstream),
                                    boxes.get(downstream),
                                    LABEL_UPSTREAM,
                                    LABEL_DOWNSTREAM))));
  }
}
