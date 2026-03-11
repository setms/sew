package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.swe.domain.model.sdlc.design.FieldConstraint.NONEMPTY;
import static org.setms.swe.domain.model.sdlc.design.FieldType.TEXT;

import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.Input;
import org.setms.swe.domain.model.sdlc.design.Entity;

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
}
