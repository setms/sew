package org.setms.km.domain.model.workspace;

import java.util.regex.Pattern;

public record Glob(String path, String pattern) {

  public static final String PATTERN_PREFIX = "**/*";

  public String extension() {
    return pattern.startsWith(PATTERN_PREFIX) ? pattern.substring(PATTERN_PREFIX.length()) : null;
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
    var regex = "";
    var remainder = pattern;
    if (remainder.startsWith("**/")) {
      regex = ".*";
      remainder = remainder.substring(3);
    }
    if (remainder.startsWith("*.")) {
      regex += "[^/]*\\.";
      remainder = remainder.substring(2);
    }
    regex += remainder;
    var matcher = Pattern.compile(regex).matcher(candidate);
    return matcher.matches();
  }

  @Override
  public String toString() {
    return "%s/%s".formatted(path, pattern);
  }
}
