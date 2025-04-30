package org.setms.sew.core.domain.model.tool;

public record Glob(String path, String pattern) {

  @Override
  public String toString() {
    return "%s/%s".formatted(path, pattern);
  }
}
