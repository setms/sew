package org.setms.sew.core.inbound.tool;

import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.sew.core.inbound.tool.Inputs.domains;
import static org.setms.sew.core.inbound.tool.Inputs.modules;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.view.mxGraph;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.swing.SwingConstants;
import lombok.extern.slf4j.Slf4j;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.sew.core.domain.model.sdlc.architecture.Module;
import org.setms.sew.core.domain.model.sdlc.architecture.Modules;
import org.setms.sew.core.domain.model.sdlc.ddd.Domain;
import org.setms.sew.core.domain.model.sdlc.ddd.Subdomain;

@Slf4j
public class ModulesTool extends BaseTool<Modules> {

  private static final String VERTEX_STYLE = "shape=rectangle;fontColor=#6482B9;fillColor=none;";
  private static final int MAX_TEXT_LENGTH = 15;

  @Override
  public Input<Modules> getMainInput() {
    return modules();
  }

  @Override
  public Set<Input<?>> getAdditionalInputs() {
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
    var report = resource.select(modules.getName() + ".html");
    try (var writer = new PrintWriter(report.writeTo())) {
      writer.println("<html>");
      writer.println("  <body>");
      build(modules, toGraph(modules, domains), resource, diagnostics)
          .ifPresent(
              image ->
                  writer.printf(
                      "    <img src=\"%s\" width=\"100%%\">%n",
                      report.toUri().resolve(".").normalize().relativize(image.toUri())));
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private mxGraph toGraph(Modules modules, List<Domain> domains) {
    var result = new mxGraph();
    result.getModel().beginUpdate();
    try {
      buildGraph(modules, result, domains);
      layoutGraph(result);
    } finally {
      result.getModel().endUpdate();
    }
    return result;
  }

  private void buildGraph(Modules modules, mxGraph graph, List<Domain> domains) {
    var verticesByModule = new HashMap<Module, Object>();
    modules.getModules().forEach(module -> verticesByModule.put(module, addVertex(module, graph)));
    modules
        .getModules()
        .forEach(
            source ->
                dependenciesOf(source, modules, domains)
                    .forEach(target -> addEdge(source, target, verticesByModule, graph)));
  }

  private Stream<Module> dependenciesOf(Module module, Modules modules, List<Domain> domains) {
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

  private Object addVertex(Module module, mxGraph graph) {
    return graph.insertVertex(
        graph.getDefaultParent(),
        null,
        wrap(module.getName(), MAX_TEXT_LENGTH),
        0,
        0,
        120,
        60,
        VERTEX_STYLE);
  }

  private void addEdge(
      Module source, Module target, Map<Module, Object> verticesByModule, mxGraph graph) {
    var from = verticesByModule.get(source);
    var to = verticesByModule.get(target);
    graph.insertEdge(graph.getDefaultParent(), null, "", from, to);
  }

  private void layoutGraph(mxGraph graph) {
    var layout = new mxHierarchicalLayout(graph, SwingConstants.NORTH);
    layout.execute(graph.getDefaultParent());
  }
}
