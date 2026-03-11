package org.setms.swe.domain.model.sdlc.database;

import java.util.Collection;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.swe.domain.model.sdlc.technology.NameExtractor;

/** A database technology that provides database schema files. */
public interface DatabaseTechnology extends NameExtractor {

  Collection<Glob> databaseSchemas();
}
