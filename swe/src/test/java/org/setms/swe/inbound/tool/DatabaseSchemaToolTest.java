package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.swe.domain.model.sdlc.architecture.BuildSystem;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.Framework;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;

class DatabaseSchemaToolTest extends ResolverToolTestCase<DatabaseSchema> {

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

  @Test
  void shouldWarnAboutMissingEntityObject() {
    var schema = new DatabaseSchema(new FullyQualifiedName("db", "TodoItem"));
    var inputs = givenInputsForSpringBoot();
    var diagnostics = new ArrayList<Diagnostic>();

    ((DatabaseSchemaTool) getTool()).validate(schema, inputs, diagnostics);

    assertThatDiagnosticsWarnAboutMissingEntityObject(diagnostics);
  }

  private ResolvedInputs givenInputsForSpringBoot() {
    return givenInputsWithAllPrerequisites().put("decisions", springBootDecisions());
  }

  private List<Decision> springBootDecisions() {
    return List.of(
        newDecision(BuildSystem.TOPIC, "Gradle"), newDecision(Framework.TOPIC, "Spring Boot"));
  }

  private static Decision newDecision(String topic, String choice) {
    return new Decision(new FullyQualifiedName("technology", topic))
        .setTopic(topic)
        .setChoice(choice);
  }

  private void assertThatDiagnosticsWarnAboutMissingEntityObject(
      Collection<Diagnostic> diagnostics) {
    assertThat(diagnostics)
        .extracting(Diagnostic::level, Diagnostic::message)
        .as("DatabaseSchemaTool should warn about missing entity object for TodoItem")
        .containsExactly(tuple(WARN, "Missing entity object"));
    assertThat(diagnostics)
        .flatExtracting(Diagnostic::suggestions)
        .extracting(Suggestion::message)
        .as("DatabaseSchemaTool should suggest creating entity object")
        .containsExactly("Create entity object");
  }
}
