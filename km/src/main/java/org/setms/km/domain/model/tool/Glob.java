package org.setms.km.domain.model.tool;

public record Glob(String path, String pattern) {

  public static final String PATTERN_PREFIX = "**/*";

  public String extension() {
    return pattern.startsWith(PATTERN_PREFIX) ? pattern.substring(PATTERN_PREFIX.length()) : null;
  }

  @Override
  public String toString() {
    return "%s/%s".formatted(path, pattern);
  }
}
