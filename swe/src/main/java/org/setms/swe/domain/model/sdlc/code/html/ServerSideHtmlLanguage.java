package org.setms.swe.domain.model.sdlc.code.html;

import static java.util.Collections.emptyList;

import java.util.Collection;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.swe.domain.model.sdlc.architecture.TopicProvider;
import org.setms.swe.domain.model.sdlc.architecture.UserInterface;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguageConventions;

public class ServerSideHtmlLanguage implements TopicProvider, ProgrammingLanguageConventions {

  @Override
  public boolean isValidChoice(String topic, String choice) {
    return UserInterface.TOPIC.equals(topic) && "ServerSide".equals(choice);
  }

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
