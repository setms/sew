package org.setms.sew.core.tool;

public record Glob(String path, String pattern) {

  @Override
  public String toString() {
    return "%s/%s".formatted(path, pattern);
  }
}
