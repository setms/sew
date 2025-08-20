package org.setms.km.domain.model.diagram;

import lombok.Getter;

public final class ShapeBox extends Box {

  @Getter private final Shape shape;
  @Getter private final int width;
  @Getter private final int height;

  public ShapeBox(String text, Shape shape, int width, int height) {
    super(text);
    this.shape = shape;
    this.width = width;
    this.height = height;
  }
}
