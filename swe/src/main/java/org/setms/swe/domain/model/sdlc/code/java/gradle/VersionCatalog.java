package org.setms.swe.domain.model.sdlc.code.java.gradle;

import java.util.Optional;

class VersionCatalog {

  private static final String VERSION_REF = ", version.ref = \"%s\"";
  private static final String LIBRARY =
      """
      %s = { module = "%s:%s"%s }""";
  private static final String VERSION =
      """
      [versions]
      %s = "%s"
      """;

  private String content;

  VersionCatalog(String content) {
    this.content = content;
  }

  public void addDependency(String module, String artifact, String version) {
    if (version != null) {
      content = content.replace("[versions]", VERSION.formatted(artifact, version));
    }
    var versionRef =
        Optional.ofNullable(version)
            .map(ignored -> artifact)
            .map(VERSION_REF::formatted)
            .orElse("");
    var dependency = LIBRARY.formatted(artifact, module, artifact, versionRef);
    content = content.replace("[libraries]", "[libraries]\n%s".formatted(dependency));
  }

  @Override
  public String toString() {
    return content;
  }
}
