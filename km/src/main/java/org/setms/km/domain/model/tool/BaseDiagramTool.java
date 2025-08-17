package org.setms.km.domain.model.tool;

import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.view.mxGraph;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

public abstract class BaseDiagramTool<A extends Artifact> extends Tool<A> {

  protected Optional<Resource<?>> build(
      Artifact object, mxGraph graph, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    try {
      var image = renderGraph(graph);
      if (image == null) {
        return Optional.empty();
      }
      var result = resource.select(object.getName() + ".png");
      try (var output = result.writeTo()) {
        ImageIO.write(image, "PNG", output);
      }
      return Optional.of(result);
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
    return Optional.empty();
  }

  private BufferedImage renderGraph(mxGraph graph) {
    var image = mxCellRenderer.createBufferedImage(graph, null, 1, null, true, null);
    if (image == null) {
      return null;
    }
    var result =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    var graphics = result.getGraphics();
    graphics.drawImage(image, 0, 0, null);
    graphics.dispose();
    return result;
  }
}
