package org.setms.km.domain.model.diagram;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Optional;

public abstract class BaseDiagramRenderer implements DiagramRenderer {

  @Override
  public BufferedImage render(Diagram diagram) {
    return Optional.ofNullable(diagram)
        .map(this::doRender)
        .map(this::reRenderWithTransparentBackground)
        .orElse(null);
  }

  protected BufferedImage doRender(Diagram diagram) {
    return doRender(diagram, diagram.normalizeTexts());
  }

  protected abstract BufferedImage doRender(Diagram diagram, int numBoxTextLines);

  protected URL loadIcon(String iconPath, String fallbackPath) {
    return Optional.ofNullable(loadIcon(iconPath)).orElseGet(() -> loadIcon(fallbackPath));
  }

  private URL loadIcon(String path) {
    return getClass().getClassLoader().getResource(path + ".png");
  }

  private BufferedImage reRenderWithTransparentBackground(BufferedImage image) {
    var result =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
    var graphics = result.getGraphics();
    graphics.drawImage(image, 0, 0, null);
    graphics.dispose();
    return result;
  }
}
