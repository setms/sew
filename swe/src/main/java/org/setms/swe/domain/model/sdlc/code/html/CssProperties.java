package org.setms.swe.domain.model.sdlc.code.html;

import static java.util.Map.entry;

import java.util.Map;
import java.util.Optional;

class CssProperties {

  record CssProperty(String selector, String name) {}

  private static final Map<String, CssProperty> CSS_PROPERTIES_BY_DESIGN_SYSTEM_PROPERTY =
      Map.ofEntries(
          entry("ButtonFontSize", new CssProperty("button", "font-size")),
          entry("ButtonFontWeight", new CssProperty("button", "font-weight")),
          entry("ButtonTextColor", new CssProperty("button", "color")),
          entry("ButtonPaddingVertical", new CssProperty("button", "padding-block")),
          entry("ButtonPaddingHorizontal", new CssProperty("button", "padding-inline")),
          entry("ButtonBorderRadius", new CssProperty("button", "border-radius")),
          entry("ButtonBackgroundColor", new CssProperty("button", "background-color")),
          entry("Gap", new CssProperty("form", "gap")),
          entry("InputFontSize", new CssProperty("input", "font-size")),
          entry("InputFontWeight", new CssProperty("input", "font-weight")),
          entry("InputTextColor", new CssProperty("input", "color")),
          entry("InputPaddingVertical", new CssProperty("input", "padding-block")),
          entry("InputPaddingHorizontal", new CssProperty("input", "padding-inline")),
          entry("InputBorderWidth", new CssProperty("input", "border-width")),
          entry("InputBorderColor", new CssProperty("input", "border-color")),
          entry("InputBorderRadius", new CssProperty("input", "border-radius")),
          entry("InputBackgroundColor", new CssProperty("input", "background-color")),
          entry("LabelFontSize", new CssProperty("label", "font-size")),
          entry("LabelFontWeight", new CssProperty("label", "font-weight")),
          entry("LabelTextColor", new CssProperty("label", "color")),
          entry("LabelMarginBottom", new CssProperty("label", "margin-bottom")));

  static Optional<CssProperty> of(String designSystemPropertyName) {
    return Optional.ofNullable(
        CSS_PROPERTIES_BY_DESIGN_SYSTEM_PROPERTY.get(designSystemPropertyName));
  }
}
