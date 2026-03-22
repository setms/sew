package org.setms.swe.domain.model.sdlc.code.html;

import static java.util.Collections.emptyList;

import java.util.Collection;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.swe.domain.model.sdlc.architecture.TopicProvider;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguageConventions;

public class ServerSideCssLanguage implements TopicProvider, ProgrammingLanguageConventions {

  @Override
  public Type type() {
    return Type.FRONTEND;
  }

  @Override
  public Collection<Glob> buildConfigurationFiles() {
    return emptyList();
  }

  @Override
  public String extension() {
    return "css";
  }

  @Override
  public String codePath() {
    return "src/main/resources/static/css";
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
    // Name isn't stored inside .css file, must come from outside
    return null;
  }
}
