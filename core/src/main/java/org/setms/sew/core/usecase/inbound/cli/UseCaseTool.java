package org.setms.sew.core.usecase.inbound.cli;

import static org.setms.sew.core.tool.Level.ERROR;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.setms.sew.core.format.sew.SewFormat;
import org.setms.sew.core.schema.Pointer;
import org.setms.sew.core.stakeholders.inbound.cli.UseCase;
import org.setms.sew.core.tool.Diagnostic;
import org.setms.sew.core.tool.Glob;
import org.setms.sew.core.tool.Input;
import org.setms.sew.core.tool.Output;
import org.setms.sew.core.tool.ResolvedInputs;
import org.setms.sew.core.tool.Tool;

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
  public void build(ResolvedInputs inputs, File outputDir, Collection<Diagnostic> diagnostics) {
    var useCases = inputs.get("useCases", UseCase.class);
    var reportDir = new File(outputDir, "reports/useCases");
    useCases.forEach(useCase -> build(useCase, reportDir, diagnostics));
  }

  private void build(UseCase useCase, File outputDir, Collection<Diagnostic> diagnostics) {
    useCase.getScenarios().forEach(scenario -> build(scenario, outputDir, diagnostics));
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void build(
      UseCase.Scenario scenario, File outputDir, Collection<Diagnostic> diagnostics) {
    try {
      var file = new File(outputDir, scenario.getName() + ".png");
      file.getParentFile().mkdirs();
      var image = render(scenario);
      ImageIO.write(image, "PNG", file);
    } catch (IOException e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
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
        "shape=image;image=%s;verticalLabelPosition=bottom;verticalAlign=top;fontColor=#000000"
            .formatted(url.toExternalForm()));
  }
}
