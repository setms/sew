package org.setms.km.domain.model.diagram;

import lombok.Getter;

public final class ShapeBox extends Box {

  @Getter private final Shape shape;

  public ShapeBox(String text, Shape shape) {
    super(text);
    this.shape = shape;
  }
}
