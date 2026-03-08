package org.setms.swe.domain.model.sdlc.packaging;

import java.util.Collection;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.swe.domain.model.sdlc.technology.NameExtractor;

public interface Packager extends NameExtractor {

  Collection<Glob> packagingDescriptions();
}
