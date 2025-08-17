package org.setms.km.domain.model.diagram;

import java.awt.image.BufferedImage;

public interface DiagramRenderer {

  String getId();

  BufferedImage render(Diagram diagram);
}
