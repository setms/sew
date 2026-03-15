package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.technology.Database;
import org.setms.swe.domain.model.sdlc.technology.FrameworkCodeGenerator;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

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
    return givenInputsWithAllPrerequisites().put("decisions", newSpringBootDecisions());
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

  @Test
  void shouldCreateEntityCode() {
    var generator = mock(FrameworkCodeGenerator.class);
    var resolver = mock(TechnologyResolver.class);
    when(resolver.frameworkCodeGenerator(any(), any())).thenReturn(Optional.of(generator));
    var database = mock(Database.class);
    when(resolver.database(any(), any())).thenReturn(Optional.of(database));
    when(generator.generateEntityFor(any(), any(), any())).thenReturn(List.of());
    var schema = new DatabaseSchema(new FullyQualifiedName("db", "TodoItem"));

    new DatabaseSchemaTool(resolver)
        .applySuggestion(
            schema,
            DatabaseSchemaTool.CREATE_ENTITY,
            null,
            new ResolvedInputs(),
            new InMemoryWorkspace().root());

    verify(generator).generateEntityFor(eq(schema), any(), any());
  }
}
