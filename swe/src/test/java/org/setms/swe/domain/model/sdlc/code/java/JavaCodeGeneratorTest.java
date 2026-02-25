package org.setms.swe.domain.model.sdlc.code.java;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;

class JavaCodeGeneratorTest {

  private static final String PACKAGE = "project";

  @Test
  void shouldNotDuplicateCommandPackageWhenTopLevelPackageEndsWithSameSegment() {
    var generator = new JavaCodeGenerator("com.company.project");
    var payload = givenPayload();
    var command =
        new Command(new FullyQualifiedName(PACKAGE, "CreateProject"))
            .setDisplay("Create Project")
            .setPayload(new Link("entity", payload.getName()));

    var actual = generator.generate(command, payload);

    assertThat(actual).hasSize(1);
    assertThatGeneratedCodeImplementsCommand(actual.getFirst());
  }

  private Entity givenPayload() {
    return new Entity(new FullyQualifiedName(PACKAGE, "Project"))
        .setFields(
            List.of(
                new Field(new FullyQualifiedName(PACKAGE, "Name")).setType(FieldType.TEXT),
                new Field(new FullyQualifiedName(PACKAGE, "Description")).setType(FieldType.TEXT)));
  }

  private void assertThatGeneratedCodeImplementsCommand(CodeArtifact actual) {
    assertThat(actual.getPackage()).isEqualTo("com.company.project.domain.model");
    assertThat(actual.getName()).isEqualTo("CreateProject");
    assertThat(actual.getCode())
        .isEqualTo(
            """
            package com.company.project.domain.model;

            public record CreateProject(String name, String description) {}
            """);
  }
}
