package org.setms.km.domain.model.diagram;

import java.util.Collection;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

public class FooDiagramTool extends BaseDiagramTool<Foo> {

  @Override
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    buildHtml(
        new Foo(new FullyQualifiedName("ape.Bear")),
        "Let me splain it to you",
        newDiagram(),
        resource,
        diagnostics);
  }

  private Diagram newDiagram() {
    var result = new Diagram();
    var from = result.add(new IconBox("Home", "icons/home"));
    var to = result.add(new IconBox("Restaurant", "icons/restaurant"));
    result.add(new Arrow(from, to, "to"));
    return result;
  }
}
