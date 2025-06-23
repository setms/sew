package org.setms.sew.core.inboud.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.sew.core.domain.model.sdlc.design.FieldConstraint.NONEMPTY;
import static org.setms.sew.core.domain.model.sdlc.design.FieldType.TEXT;

import org.setms.sew.core.domain.model.sdlc.design.Entity;

class EntityToolTest extends ToolTestCase<Entity> {

  public EntityToolTest() {
    super(new EntityTool(), Entity.class, "main/design");
  }

  @Override
  protected void assertThatParsedObjectMatchesExpectations(Entity parsed) {
    assertThat(parsed.getFields())
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
