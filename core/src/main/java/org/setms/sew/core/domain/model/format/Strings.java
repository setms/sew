package org.setms.sew.core.domain.model.format;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Strings {

  private static final Collection<Character> FORBIDDEN = List.of('\'', '[', ']');
  private static final Map<String, String> REPLACEMENTS =
      Map.of("cant", "can't", "doesnt", "doesn't", "isnt", "isn't");

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
    if (name == null || name.isBlank()) {
      return "";
    }
    var builder = new StringBuilder(name);
    for (var i = 1; i < builder.length(); i++) {
      if (Character.isUpperCase(builder.charAt(i))) {
        builder.setCharAt(i, Character.toLowerCase(builder.charAt(i)));
        builder.insert(i, ' ');
      }
    }
    var result = new AtomicReference<>(builder.toString());
    REPLACEMENTS.forEach(
        (text, replacement) -> result.set(result.get().replace(text, replacement)));
    return result.get();
  }

  public static String ensureSuffix(String text, String suffix) {
    return text.endsWith(suffix) ? text : text + suffix;
  }
}
