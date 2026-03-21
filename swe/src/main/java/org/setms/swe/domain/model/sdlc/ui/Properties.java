package org.setms.swe.domain.model.sdlc.ui;

import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Properties {

  private static final Map<String, String> DEFAULT_VALUES = new HashMap<>();

  static {
    DEFAULT_VALUES.put("ButtonFontSize", "14px");
    DEFAULT_VALUES.put("ButtonFontWeight", "600");
    DEFAULT_VALUES.put("ButtonTextColor", "#FFFFFF");
    DEFAULT_VALUES.put("ButtonPaddingVertical", "10px");
    DEFAULT_VALUES.put("ButtonPaddingHorizontal", "14px");
    DEFAULT_VALUES.put("ButtonBorderRadius", "8px");
    DEFAULT_VALUES.put("ButtonBackgroundColor", "#2563EB");

    DEFAULT_VALUES.put("Gap", "16px");

    DEFAULT_VALUES.put("InputFontSize", "14px");
    DEFAULT_VALUES.put("InputFontWeight", "400");
    DEFAULT_VALUES.put("InputTextColor", "#1F2937");
    DEFAULT_VALUES.put("InputPaddingVertical", "10px");
    DEFAULT_VALUES.put("InputPaddingHorizontal", "14px");
    DEFAULT_VALUES.put("InputBorderWidth", "1px");
    DEFAULT_VALUES.put("InputBorderColor", "#D1D5DB");
    DEFAULT_VALUES.put("InputBorderRadius", "8px");
    DEFAULT_VALUES.put("InputBackgroundColor", "#FFFFFF");

    DEFAULT_VALUES.put("LabelFontSize", "14px");
    DEFAULT_VALUES.put("LabelFontWeight", "500");
    DEFAULT_VALUES.put("LabelTextColor", "#1F2937");
    DEFAULT_VALUES.put("LabelMarginBottom", "6px");
  }

  public static Collection<String> names() {
    return DEFAULT_VALUES.keySet();
  }

  public static String defaultFor(String name) {
    return DEFAULT_VALUES.get(name);
  }
}
