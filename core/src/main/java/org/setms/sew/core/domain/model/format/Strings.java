package org.setms.sew.core.domain.model.format;

import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Strings {

  private static final Collection<Character> FORBIDDEN = List.of('\'', '[', ']');

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

  public static String toObjectName(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    var result = new StringBuilder(value);
    result.insert(0, ' ');
    for (var i = 0; i < result.length(); i++) {
      if (Character.isWhitespace(result.charAt(i))) {
        result.delete(i, i + 1);
        result.setCharAt(i, Character.toUpperCase(result.charAt(i)));
      } else if (FORBIDDEN.contains(result.charAt(i))) {
        result.delete(i, i + 1);
        i--;
      }
    }
    return result.toString();
  }

  public static String toFriendlyName(String name) {
    var result = new StringBuilder(name);
    for (var i = 1; i < result.length(); i++) {
      if (Character.isUpperCase(result.charAt(i))) {
        result.setCharAt(i, Character.toLowerCase(result.charAt(i)));
        result.insert(i, ' ');
      }
    }
    return result.toString();
  }
}
