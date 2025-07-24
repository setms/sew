package org.setms.km.domain.model.format;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Strings {

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
