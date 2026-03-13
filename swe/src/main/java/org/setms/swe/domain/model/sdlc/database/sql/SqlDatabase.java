package org.setms.swe.domain.model.sdlc.database.sql;

import static org.setms.km.domain.model.format.Strings.toPascalCase;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.swe.domain.model.sdlc.database.DatabaseTechnology;

/** SQL-based database technology. */
public class SqlDatabase implements DatabaseTechnology {

  private static final Pattern TABLE_NAME_PATTERN =
      Pattern.compile("(?i)\\bCREATE\\s+TABLE\\s+(\\w+)");

  @Override
  public Collection<Glob> databaseSchemas() {
    return List.of(Glob.of("src/main/design/physical", "sql"));
  }

  @Override
  public FullyQualifiedName extractName(String code) {
    var matcher = TABLE_NAME_PATTERN.matcher(code);
    return matcher.find()
        ? new FullyQualifiedName("database", toPascalCase(matcher.group(1)))
        : null;
  }
}
