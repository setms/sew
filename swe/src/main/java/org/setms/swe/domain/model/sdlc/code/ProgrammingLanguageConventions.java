package org.setms.swe.domain.model.sdlc.code;

import java.util.Collection;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.workspace.Glob;

public interface ProgrammingLanguageConventions {

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

  FullyQualifiedName extractName(String code);
}
