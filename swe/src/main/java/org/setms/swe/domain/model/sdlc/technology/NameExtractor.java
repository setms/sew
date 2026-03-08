package org.setms.swe.domain.model.sdlc.technology;

import org.setms.km.domain.model.artifact.FullyQualifiedName;

public interface NameExtractor {

  FullyQualifiedName extractName(String code);
}
