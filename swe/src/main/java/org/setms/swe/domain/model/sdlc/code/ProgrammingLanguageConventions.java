package org.setms.swe.domain.model.sdlc.code;

import java.util.Collection;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.swe.domain.model.sdlc.technology.NameExtractor;

public interface ProgrammingLanguageConventions extends NameExtractor {

  enum Type {
    BACKEND,
    FRONTEND
  }

  Type type();

  Collection<Glob> buildConfigurationFiles();

  String extension();

  String codePath();

  String unitTestPath();

  String unitTestPattern();

  String unitTestHelpersPattern();

  default Glob unitTestHelpersGlob() {
    return new Glob(unitTestPath(), unitTestHelpersPattern())
        .excluding(new Glob(unitTestPath(), unitTestPattern()));
  }
}
