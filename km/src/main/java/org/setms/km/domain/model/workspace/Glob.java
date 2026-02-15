package org.setms.km.domain.model.workspace;

import java.util.regex.Pattern;

public record Glob(String path, String pattern) {

  public static final String PATTERN_PREFIX = "**/*.";

  public static Glob of(String path, String extension) {
    return new Glob(path, PATTERN_PREFIX + extension);
  }

  public String extension() {
    var index = pattern.lastIndexOf('.');
    return index >= 0 ? pattern.substring(index + 1) : null;
  }

  public boolean matches(String path) {
    var candidate = path;
    var index = path.isEmpty() ? 0 : candidate.indexOf(this.path);
    if (index < 0) {
      return false;
    }
    index += this.path.length();
    if (candidate.length() <= index) {
      return false;
    }
    candidate = candidate.substring(index);
    var regex = patternToRegex(pattern);
    var matcher = Pattern.compile(regex).matcher(candidate);
    return matcher.matches();
  }

  private String patternToRegex(String pattern) {
    var result = new StringBuilder();

    var remainder = pattern;
    if (remainder.startsWith("**/")) {
      result.append(".*");
      remainder = remainder.substring(3);
    }

    while (!remainder.isEmpty()) {
      var starIndex = remainder.indexOf('*');
      if (starIndex < 0) {
        result.append(Pattern.quote(remainder));
        break;
      }
      if (starIndex > 0) {
        result.append(Pattern.quote(remainder.substring(0, starIndex)));
      }
      result.append("[^/]*");
      remainder = remainder.substring(starIndex + 1);
    }

    return result.toString();
  }

  @Override
  public String toString() {
    return "%s/%s".formatted(path, pattern);
  }
}
