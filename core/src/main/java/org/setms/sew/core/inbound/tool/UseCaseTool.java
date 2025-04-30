package org.setms.sew.core.inbound.tool;

import static org.setms.sew.core.domain.model.tool.Level.ERROR;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.setms.sew.core.inbound.format.sew.SewFormat;
import org.setms.sew.core.domain.model.schema.Pointer;
import org.setms.sew.core.domain.model.ddd.UseCase;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Tool;

public class UseCaseTool extends Tool {

  private static final String OUTPUT_PATH = "build/reports/useCases";
  public static final int ICON_SIZE = 80;

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>(
            "useCases",
            new Glob("src/main/requirements", "**/*.useCase"),
            new SewFormat(),
            UseCase.class));
  }

  @Override
  public List<Output> getOutputs() {
    return List.of(
        new Output(new Glob(OUTPUT_PATH, "*.html")), new Output(new Glob(OUTPUT_PATH, "*.png")));
  }

  @Override
  public void build(ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var useCases = inputs.get("useCases", UseCase.class);
    var reportSink = sink.select("reports/useCases");
    useCases.forEach(useCase -> build(useCase, reportSink, diagnostics));
  }

  private void build(UseCase useCase, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var report = sink.select(useCase.getName() + ".html");
    try (var writer = new PrintWriter(report.open())) {
      writer.println("<html>");
      writer.println("  <body>");
      writer.printf("    <h1>%s</h1>%n", useCase.getTitle());
      writer.printf("    <p>%s</p>%n", useCase.getDescription());
      useCase
          .getScenarios()
          .forEach(
              scenario -> {
                writer.printf("    <h2>%s</h2>%n", scenario.getTitle());
                writer.printf("    <p>%s</p>%n", scenario.getDescription());
                var image = build(scenario, sink, diagnostics);
                writer.printf(
                    "    <img src=\"%s\"/>%n",
                    report.toUri().resolve(".").normalize().relativize(image.toUri()));
              });
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
  }

  private OutputSink build(
      UseCase.Scenario scenario, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var result = sink.select(scenario.getName() + ".png");
    try {
      var image = render(scenario);
      ImageIO.write(image, "PNG", result.open());
    } catch (IOException e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
    return result;
  }

  private RenderedImage render(UseCase.Scenario scenario) {
    var graph = toGraph(scenario);
    var image = mxCellRenderer.createBufferedImage(graph, null, 1, null, true, null);
    var result =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    result.getGraphics().drawImage(image, 0, 0, null);
    return result;
  }

  private mxGraph toGraph(UseCase.Scenario scenario) {
    var result = new mxGraph();
    result.getModel().beginUpdate();
    try {
      var from = new AtomicReference<>(addVertex(result, scenario.getSteps().getFirst()));
      scenario.getSteps().stream()
          .skip(1)
          .forEach(
              step -> {
                var to = addVertex(result, step);
                result.insertEdge(result.getDefaultParent(), null, "", from.get(), to);
                from.set(to);
              });

      var layout = new mxHierarchicalLayout(result);
      layout.setOrientation(7); // left-to-right
      layout.setInterRankCellSpacing(ICON_SIZE / 2.0);
      layout.setIntraCellSpacing(ICON_SIZE / 4.0);
      layout.execute(result.getDefaultParent());
    } finally {
      result.getModel().endUpdate();
    }

    return result;
  }

  private Object addVertex(mxGraph graph, Pointer step) {
    var url = getClass().getClassLoader().getResource("resin/" + step.getType() + ".png");
    if (url == null) {
      throw new IllegalArgumentException("Icon not found for " + step.getType());
    }
    return graph.insertVertex(
        graph.getDefaultParent(),
        null,
        step.getId(),
        0,
        0,
        ICON_SIZE,
        ICON_SIZE,
        "shape=image;image=%s;verticalLabelPosition=bottom;verticalAlign=top;fontColor=#6482B9"
            .formatted(url.toExternalForm()));
  }
}
