package org.setms.km.domain.model.diagram;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public abstract sealed class Box permits IconBox, ShapeBox {

  @Getter @Setter private String text;
}
