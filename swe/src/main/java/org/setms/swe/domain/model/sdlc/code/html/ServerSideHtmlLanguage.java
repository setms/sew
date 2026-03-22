package org.setms.swe.domain.model.sdlc.code.html;

import java.util.Collection;
import java.util.List;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguageConventions;

public class ServerSideHtmlLanguage implements ProgrammingLanguageConventions {

  @Override
  public Collection<Glob> buildConfigurationFiles() {
    return List.of();
  }

  @Override
  public String extension() {
    return "html";
  }

  @Override
  public String codePath() {
    return "src/main/resources/templates";
  }

  @Override
  public String unitTestPath() {
    return "";
  }

  @Override
  public String unitTestPattern() {
    return "";
  }

  @Override
  public String unitTestHelpersPattern() {
    return "";
  }

  @Override
  public FullyQualifiedName extractName(String code) {
    return new FullyQualifiedName("", "");
  }
}
