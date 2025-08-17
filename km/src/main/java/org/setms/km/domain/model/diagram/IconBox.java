package org.setms.km.domain.model.diagram;

import lombok.Getter;

@Getter
public final class IconBox extends Box {

  private final String iconPath;
  private final String fallbackPath;

  public IconBox(String text, String iconPath) {
    this(text, iconPath, null);
  }

  public IconBox(String text, String iconPath, String fallbackPath) {
    super(text);
    this.iconPath = iconPath;
    this.fallbackPath = fallbackPath;
  }
}
