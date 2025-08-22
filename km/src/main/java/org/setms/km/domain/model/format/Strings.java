package org.setms.km.domain.model.format;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Strings {

  public static final String NL = System.lineSeparator();

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

  public static String wrap(String text, int maxLength) {
    if (text == null) {
      return "";
    }
    var capacity = text.length() + text.length() / maxLength;
    var result = new StringBuilder(capacity);
    result.append(text);
    var columnEnd = maxLength;
    while (columnEnd < result.length()) {
      var spaceIndex = result.lastIndexOf(" ", columnEnd);
      if (spaceIndex < columnEnd - maxLength) {
        result.insert(columnEnd, NL);
        columnEnd += maxLength + 1;
      } else {
        result.delete(spaceIndex, spaceIndex + 1);
        result.insert(spaceIndex, NL);
        columnEnd = spaceIndex + 1 + maxLength;
      }
    }
    return result.toString();
  }

  public static int numLinesIn(String text) {
    return text.split(NL).length;
  }
}
