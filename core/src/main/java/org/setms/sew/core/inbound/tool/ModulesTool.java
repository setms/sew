package org.setms.sew.core.inbound.tool;

import static org.setms.sew.core.domain.model.validation.Level.ERROR;
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
import java.util.stream.Stream;
import javax.swing.SwingConstants;
import lombok.extern.slf4j.Slf4j;
import org.setms.sew.core.domain.model.sdlc.architecture.Module;
import org.setms.sew.core.domain.model.sdlc.architecture.Modules;
import org.setms.sew.core.domain.model.sdlc.ddd.Domain;
import org.setms.sew.core.domain.model.sdlc.ddd.Subdomain;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.domain.model.validation.Diagnostic;
import org.setms.sew.core.domain.model.validation.Location;

@Slf4j
public class ModulesTool extends Tool {

  private static final String OUTPUT_PATH = "build/reports/modules";
  private static final String VERTEX_STYLE = "shape=rectangle;fontColor=#6482B9;fillColor=none;";
  private static final int MAX_TEXT_LENGTH = 15;

  @Override
  public List<Input<?>> getInputs() {
    return List.of(modules(), domains());
  }

  @Override
  public List<Output> getOutputs() {
    return htmlWithImages(OUTPUT_PATH);
  }

  @Override
  protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var domains = inputs.get(Domain.class);
    inputs
        .get(Modules.class)
        .forEach(
            modules -> {
              var location = new Location(modules);
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
                diagnostics.add(
                    new Diagnostic(
                        ERROR, "Must map to subdomain", new Location(modules).plus(module)));
              } else if (module.getMappedTo().resolveFrom(domain.getSubdomains()).isEmpty()) {
                diagnostics.add(
                    new Diagnostic(
                        ERROR,
                        "Unknown subdomain %s".formatted(module.getMappedTo().getId()),
                        new Location(modules).plus(module)));
              }
            });
  }

  @Override
  protected void build(ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var domains = inputs.get(Domain.class);
    inputs.get(Modules.class).forEach(modules -> build(modules, sink, diagnostics, domains));
  }

  private void build(
      Modules modules, OutputSink sink, Collection<Diagnostic> diagnostics, List<Domain> domains) {
    var report = sink.select(modules.getName() + ".html");
    try (var writer = new PrintWriter(report.open())) {
      writer.println("<html>");
      writer.println("  <body>");
      build(modules, toGraph(modules, domains), sink, diagnostics)
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
                    .map(pointer -> pointer.resolveFrom(domain.getSubdomains()))
                    .flatMap(Optional::stream)
                    .map(Subdomain::dependsOn)
                    .flatMap(Collection::stream)
                    .flatMap(
                        pointer ->
                            modules.getModules().stream()
                                .filter(m -> pointer.equals(m.getMappedTo()))));
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
