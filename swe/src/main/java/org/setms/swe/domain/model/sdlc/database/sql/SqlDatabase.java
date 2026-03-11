package org.setms.swe.domain.model.sdlc.database.sql;

import java.util.Collection;
import java.util.List;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.swe.domain.model.sdlc.database.DatabaseTechnology;

/** SQL-based database technology. */
public class SqlDatabase implements DatabaseTechnology {

  @Override
  public Collection<Glob> databaseSchemas() {
    return List.of(Glob.of("src/main/database", "sql"));
  }

  @Override
  public FullyQualifiedName extractName(String code) {
    return null;
  }
}
