package org.setms.km.domain.model.diagram;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Optional;
import java.util.ServiceLoader;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;

@RequiredArgsConstructor
public abstract class BaseDiagramTool extends ArtifactTool {

  private final DiagramRenderer renderer;

  protected BaseDiagramTool() {
    this("jgraphx");
  }

  protected BaseDiagramTool(String id) {
    this(loadRenderer(id));
  }

  private static DiagramRenderer loadRenderer(String id) {
    var classLoader = DiagramRenderer.class.getClassLoader();
    DiagramRenderer result = null;
    for (var renderer : ServiceLoader.load(DiagramRenderer.class, classLoader)) {
      if (renderer.getId().equals(id)) {
        return renderer;
      }
      if (result == null) {
        result = renderer;
      }
    }
    return result;
  }

  protected void buildHtml(
      Artifact artifact,
      String description,
      Diagram diagram,
      Resource<?> parent,
      Collection<Diagnostic> diagnostics) {
    var report = parent.select(artifact.getName() + ".html");
    try (var writer = new PrintWriter(report.writeTo())) {
      writer.println("<html>");
      writer.println("  <body>");
      writer.printf("    <h1>%s</h1>%n", artifact.friendlyName());
      Optional.ofNullable(description).ifPresent(desc -> writer.printf("    <p>%s</p>%n", desc));
      build(diagram, artifact.getName(), parent, diagnostics)
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

  protected Optional<Resource<?>> build(
      Artifact object, Diagram diagram, Resource<?> parent, Collection<Diagnostic> diagnostics) {
    return build(diagram, object.getName(), parent, diagnostics);
  }

  protected Optional<Resource<?>> build(
      Diagram diagram, String name, Resource<?> parent, Collection<Diagnostic> diagnostics) {
    try {
      var image = renderer.render(diagram);
      if (image == null) {
        return Optional.empty();
      }
      var result = parent.select(name + ".png");
      try (var output = result.writeTo()) {
        ImageIO.write(image, "PNG", output);
      }
      return Optional.of(result);
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
    return Optional.empty();
  }
}
