package org.setms.sew.core.inbound.tool;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.view.mxGraph;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingConstants;
import org.setms.sew.core.domain.model.sdlc.Domain;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.UseCase;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.format.sew.SewFormat;

public class DomainsTool extends Tool {

  private static final String OUTPUT_PATH = "build/reports/domains";
  private static final String VERTEX_STYLE = "shape=rectangle;fontColor=#6482B9;fillColor=none;";
  private static final int MAX_TEXT_LENGTH = 15;

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>(
            "domains",
            new Glob("src/main/architecture", "**/*.domain"),
            new SewFormat(),
            Domain.class),
        new Input<>(
            "useCases",
            new Glob("src/main/requirements", "**/*.useCase"),
            new SewFormat(),
            UseCase.class));
  }

  @Override
  public List<Output> getOutputs() {
    return List.of(new Output(new Glob(OUTPUT_PATH, "*.png")));
  }

  @Override
  protected void build(ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var output = sink.select("reports/domains");
    inputs.get("domains", Domain.class).forEach(domain -> build(domain, output, diagnostics));
  }

  private void build(Domain domain, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var report = sink.select(domain.getName() + ".html");
    try (var writer = new PrintWriter(report.open())) {
      writer.println("<html>");
      writer.println("  <body>");
      var image = build(domain, toGraph(domain), sink, diagnostics);
      writer.printf(
          "    <img src=\"%s\" width=\"100%%\">%n",
          report.toUri().resolve(".").normalize().relativize(image.toUri()));
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private mxGraph toGraph(Domain domain) {
    var result = new mxGraph();
    result.getModel().beginUpdate();
    try {
      buildGraph(domain, result);
      layoutGraph(result);
    } finally {
      result.getModel().endUpdate();
    }
    return result;
  }

  private void buildGraph(Domain domain, mxGraph graph) {
    var verticesByDomain = new HashMap<Domain.SubDomain, Object>();
    domain
        .getSubDomains()
        .forEach(subDomain -> verticesByDomain.put(subDomain, addVertex(subDomain, graph)));
    domain
        .getSubDomains()
        .forEach(
            source ->
                source
                    .dependsOn()
                    .forEach(pointer -> addEdge(domain, source, pointer, verticesByDomain, graph)));
  }

  private Object addVertex(Domain.SubDomain domain, mxGraph graph) {
    return graph.insertVertex(
        graph.getDefaultParent(),
        null,
        wrap(domain.getName(), MAX_TEXT_LENGTH),
        0,
        0,
        120,
        60,
        VERTEX_STYLE);
  }

  private void addEdge(
      Domain domain,
      Domain.SubDomain source,
      Pointer pointer,
      Map<Domain.SubDomain, Object> verticesByDomain,
      mxGraph graph) {
    pointer
        .resolveFrom(domain.getSubDomains())
        .ifPresent(
            target -> {
              var from = verticesByDomain.get(source);
              var to = verticesByDomain.get(target);
              graph.insertEdge(graph.getDefaultParent(), null, "", from, to);
            });
  }

  private void layoutGraph(mxGraph graph) {
    var layout = new mxHierarchicalLayout(graph, SwingConstants.NORTH);
    layout.execute(graph.getDefaultParent());
  }
}
