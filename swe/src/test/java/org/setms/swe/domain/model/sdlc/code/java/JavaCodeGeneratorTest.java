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
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;

class JavaCodeGeneratorTest {

  @Test
  void shouldGenerateServiceArtifacts() {
    var generator = new JavaCodeGenerator("com.company.project");
    var aggregate = new Aggregate(new FullyQualifiedName("project", "Projects"));
    var command = new Command(new FullyQualifiedName("project", "CreateProject"));
    var event = new Event(new FullyQualifiedName("project", "ProjectCreated"));

    var actual = generator.generate(aggregate, command, event);

    assertThatGeneratedCodeIsServiceInterface(actual.get(0));
    assertThatGeneratedCodeIsServiceImplementation(actual.get(1));
  }

  private void assertThatGeneratedCodeIsServiceInterface(CodeArtifact actual) {
    assertThat(actual.getName()).isEqualTo("ProjectsService");
    assertThat(actual.getPackage()).isEqualTo("com.company.project.domain.services");
    assertThat(actual.getCode())
        .contains("public interface ProjectsService")
        .contains("import com.company.project.domain.model.CreateProject;")
        .contains("import com.company.project.domain.model.ProjectCreated;")
        .contains("ProjectCreated accept(CreateProject createProject);");
  }

  private void assertThatGeneratedCodeIsServiceImplementation(CodeArtifact actual) {
    assertThat(actual.getName()).isEqualTo("ProjectsServiceImpl");
    assertThat(actual.getPackage()).isEqualTo("com.company.project.domain.services");
    assertThat(actual.getCode())
        .contains("class ProjectsServiceImpl implements ProjectsService")
        .contains("import com.company.project.domain.model.CreateProject;")
        .contains("import com.company.project.domain.model.ProjectCreated;")
        .contains("public ProjectCreated accept(CreateProject createProject) {\n    return null;");
  }

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

  @Test
  void shouldAddImportForLocalDateTimeWhenCommandHasDateTimeField() {
    var generator = new JavaCodeGenerator("com.company.project");
    var payload = givenPayloadWithDateTimeField();
    var command =
        new Command(new FullyQualifiedName(PACKAGE, "ScheduleMeeting"))
            .setDisplay("Schedule Meeting")
            .setPayload(new Link("entity", payload.getName()));

    var actual = generator.generate(command, payload);

    assertThat(actual).hasSize(1);
    assertThatGeneratedCodeHasLocalDateTimeImport(actual.getFirst());
  }

  private Entity givenPayload() {
    return new Entity(new FullyQualifiedName(PACKAGE, "Project"))
        .setFields(
            List.of(
                new Field(new FullyQualifiedName(PACKAGE, "Name")).setType(FieldType.TEXT),
                new Field(new FullyQualifiedName(PACKAGE, "Description")).setType(FieldType.TEXT)));
  }

  private Entity givenPayloadWithDateTimeField() {
    return new Entity(new FullyQualifiedName(PACKAGE, "MeetingSchedule"))
        .setFields(
            List.of(
                new Field(new FullyQualifiedName(PACKAGE, "ScheduledAt"))
                    .setType(FieldType.DATETIME)));
  }

  private void assertThatGeneratedCodeHasLocalDateTimeImport(CodeArtifact actual) {
    assertThat(actual.getCode())
        .isEqualTo(
            """
            package com.company.project.domain.model;

            import java.time.LocalDateTime;

            public record ScheduleMeeting(LocalDateTime scheduledAt) {}
            """);
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
