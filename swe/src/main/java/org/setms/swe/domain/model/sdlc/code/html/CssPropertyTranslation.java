package org.setms.swe.domain.model.sdlc.code.html;

import static java.util.Map.entry;

import java.util.Map;
import java.util.Optional;

class CssPropertyTranslation {

  record Translation(String selector, String cssProperty) {}

  private static final Map<String, Translation> TRANSLATIONS =
      Map.ofEntries(
          entry("ButtonFontSize", new Translation("button", "font-size")),
          entry("ButtonFontWeight", new Translation("button", "font-weight")),
          entry("ButtonTextColor", new Translation("button", "color")),
          entry("ButtonPaddingVertical", new Translation("button", "padding-block")),
          entry("ButtonPaddingHorizontal", new Translation("button", "padding-inline")),
          entry("ButtonBorderRadius", new Translation("button", "border-radius")),
          entry("ButtonBackgroundColor", new Translation("button", "background-color")),
          entry("Gap", new Translation("form", "gap")),
          entry("InputFontSize", new Translation("input", "font-size")),
          entry("InputFontWeight", new Translation("input", "font-weight")),
          entry("InputTextColor", new Translation("input", "color")),
          entry("InputPaddingVertical", new Translation("input", "padding-block")),
          entry("InputPaddingHorizontal", new Translation("input", "padding-inline")),
          entry("InputBorderWidth", new Translation("input", "border-width")),
          entry("InputBorderColor", new Translation("input", "border-color")),
          entry("InputBorderRadius", new Translation("input", "border-radius")),
          entry("InputBackgroundColor", new Translation("input", "background-color")),
          entry("LabelFontSize", new Translation("label", "font-size")),
          entry("LabelFontWeight", new Translation("label", "font-weight")),
          entry("LabelTextColor", new Translation("label", "color")),
          entry("LabelMarginBottom", new Translation("label", "margin-bottom")));

  static Optional<Translation> of(String sewPropertyName) {
    return Optional.ofNullable(TRANSLATIONS.get(sewPropertyName));
  }
}
