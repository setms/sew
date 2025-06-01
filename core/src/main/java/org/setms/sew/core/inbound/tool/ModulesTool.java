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
import org.setms.sew.core.domain.model.sdlc.Modules;
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

public class ModulesTool extends Tool {

  private static final String OUTPUT_PATH = "build/reports/modules";
  private static final String VERTEX_STYLE = "shape=rectangle;fontColor=#6482B9;fillColor=none;";
  private static final int MAX_TEXT_LENGTH = 15;

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>(
            "modules",
            new Glob("src/main/architecture", "**/*.modules"),
            new SewFormat(),
            Modules.class),
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
    var output = sink.select("reports/modules");
    inputs.get("modules", Modules.class).forEach(modules -> build(modules, output, diagnostics));
  }

  private void build(Modules modules, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var report = sink.select(modules.getName() + ".html");
    try (var writer = new PrintWriter(report.open())) {
      writer.println("<html>");
      writer.println("  <body>");
      var image = build(modules, toGraph(modules), sink, diagnostics);
      writer.printf(
          "    <img src=\"%s\" width=\"100%%\">%n",
          report.toUri().resolve(".").normalize().relativize(image.toUri()));
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private mxGraph toGraph(Modules modules) {
    var result = new mxGraph();
    result.getModel().beginUpdate();
    try {
      buildGraph(modules, result);
      layoutGraph(result);
    } finally {
      result.getModel().endUpdate();
    }
    return result;
  }

  private void buildGraph(Modules modules, mxGraph graph) {
    var verticesByModule = new HashMap<Modules.Module, Object>();
    modules.getModules().forEach(module -> verticesByModule.put(module, addVertex(module, graph)));
    modules
        .getModules()
        .forEach(
            source ->
                source
                    .dependsOn()
                    .forEach(
                        pointer -> addEdge(modules, source, pointer, verticesByModule, graph)));
  }

  private Object addVertex(Modules.Module module, mxGraph graph) {
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
      Modules modules,
      Modules.Module source,
      Pointer pointer,
      Map<Modules.Module, Object> verticesByModule,
      mxGraph graph) {
    pointer
        .resolveFrom(modules.getModules())
        .ifPresent(
            target -> {
              var from = verticesByModule.get(source);
              var to = verticesByModule.get(target);
              graph.insertEdge(graph.getDefaultParent(), null, "", from, to);
            });
  }

  private void layoutGraph(mxGraph graph) {
    var layout = new mxHierarchicalLayout(graph, SwingConstants.WEST);
    layout.execute(graph.getDefaultParent());
  }
}
