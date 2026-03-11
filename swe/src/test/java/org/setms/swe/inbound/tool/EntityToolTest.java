package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.domain.model.sdlc.design.FieldConstraint.NONEMPTY;
import static org.setms.swe.domain.model.sdlc.design.FieldType.TEXT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

class EntityToolTest extends ToolTestCase<Entity> {

  public EntityToolTest() {
    super(new EntityTool(), Entity.class, "main/design");
  }

  @Override
  protected void assertValidationContext(Set<Input<? extends Artifact>> inputs) {
    assertThat(inputs)
        .as("Validation context includes database schemas")
        .anyMatch(input -> input.matches("src/main/database/shop/product.sql"));
  }

  @Override
  protected void assertThatParsedObjectMatchesExpectations(Artifact artifact) {
    var entity = (Entity) artifact;
    assertThat(entity.getFields())
        .hasSize(1)
        .allSatisfy(
            field -> {
              assertThat(field.getType()).isEqualTo(TEXT);
              assertThat(field.getConstraints())
                  .hasSize(1)
                  .allSatisfy(constraint -> assertThat(constraint).isEqualTo(NONEMPTY));
            });
  }

  @Test
  void shouldCheckDatabaseTechnologyWhenEntityHasNoDatabaseSchema() {
    var entity = new Entity(new FullyQualifiedName("design", "Product"));
    var resolver = mock(TechnologyResolver.class);
    var noDatabaseDiagnostic = givenResolverIndicatingNoDatabaseDecision(resolver);
    var inputs = new ResolvedInputs();
    var diagnostics = new ArrayList<Diagnostic>();

    new EntityTool(resolver).validate(entity, inputs, diagnostics);

    assertThat(diagnostics)
        .as("Database technology check should run when entity has no database schema")
        .containsExactly(noDatabaseDiagnostic);
  }

  private Diagnostic givenResolverIndicatingNoDatabaseDecision(TechnologyResolver resolver) {
    var diagnostic = new Diagnostic(WARN, "Missing database");
    when(resolver.database(any(), anyCollection()))
        .thenAnswer(
            invocation -> {
              Collection<Diagnostic> diagnostics = invocation.getArgument(1);
              diagnostics.add(diagnostic);
              return Optional.empty();
            });
    return diagnostic;
  }
}
