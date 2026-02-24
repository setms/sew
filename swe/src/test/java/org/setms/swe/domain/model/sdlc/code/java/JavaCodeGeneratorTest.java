package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;

class JavaCodeGeneratorTest {

  @Test
  void shouldNotDuplicateCommandPackageWhenTopLevelPackageEndsWithSameSegment() {
    var generator = new JavaCodeGenerator("com.company.project");
    var command =
        new Command(new FullyQualifiedName("project", "CreateProject"))
            .setDisplay("Create Project");

    var actual = generator.generate(command);

    assertThat(actual).hasSize(1);
    assertThat(actual.getFirst().getPackage()).isEqualTo("com.company.project.domain.model");
    assertThat(actual.getFirst().getName()).isEqualTo("CreateProject");
  }
}
