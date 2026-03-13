package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;

class DatabaseSchemaToolTest extends ToolTestCase<DatabaseSchema> {

  DatabaseSchemaToolTest() {
    super(new DatabaseSchemaTool(), null, "main/design/physical", "sql");
  }

  @Override
  protected void assertValidationTarget(ArtifactTool<?> tool) {
    assertThat(tool.validationTargets())
        .extracting(Input::path, Input::extension)
        .as("DatabaseSchemaTool should target SQL schema files at src/main/design/physical")
        .contains(tuple("src/main/design/physical", "sql"));
  }

  @Override
  protected void assertValidationContext(Set<Input<? extends Artifact>> inputs) {
    assertThat(inputs)
        .extracting(Input::path, Input::extension)
        .as(
            "DatabaseSchemaTool validation context should include Java source code at src/main/java")
        .contains(tuple("src/main/java", "java"));
  }
}
