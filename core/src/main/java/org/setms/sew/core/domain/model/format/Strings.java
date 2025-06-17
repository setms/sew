package org.setms.sew.core.domain.model.format;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Strings {

  public static String initUpper(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }

  public static String initLower(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    return Character.toLowerCase(value.charAt(0)) + value.substring(1);
  }

  public static boolean isNotBlank(String value) {
    return value != null && !value.isBlank();
  }

  public static String stripQuotesFrom(String quotedText) {
    if (quotedText == null || quotedText.length() < 2) {
      return quotedText;
    }
    return quotedText.substring(1, quotedText.length() - 1);
  }
}
