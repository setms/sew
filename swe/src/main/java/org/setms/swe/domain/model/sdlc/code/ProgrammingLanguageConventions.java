package org.setms.swe.domain.model.sdlc.code;

import org.setms.km.domain.model.artifact.FullyQualifiedName;

public interface ProgrammingLanguageConventions {

  String extension();

  String codePath();

  String unitTestPath();

  String unitTestPattern();

  String unitTestHelpersPattern();

  FullyQualifiedName extractName(String code);
}
