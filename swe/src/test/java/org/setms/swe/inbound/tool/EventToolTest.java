package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SequencedCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

class EventToolTest extends ResolverToolTestCase<Event> {

  EventToolTest() {
    super(new EventTool(), Event.class, "main/design");
  }

  @AfterEach
  void cleanupGeneratedFiles() throws IOException {
    workspaceFor("missing").root().select("src/main/design/Payload.entity").delete();
  }

  @Test
  void shouldNotWarnAboutMissingCodeWhenEventHasNoPayload() {
    var event = new Event(new FullyQualifiedName("design", "WithoutPayload"));
    var diagnostics = new ArrayList<Diagnostic>();

    ((EventTool) getTool()).validate(event, new ResolvedInputs(), diagnostics);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void shouldReportDiagnosticsFromResolverWhenCodeIsMissing() {
    var resolver = mock(TechnologyResolver.class);
    var tool = new EventTool(resolver);
    var diagnostic = givenResolverAddingDiagnostic(resolver);
    var diagnostics = new ArrayList<Diagnostic>();

    tool.validate(givenEventWithPayload(), givenResolvedPayload(), diagnostics);

    assertThat(diagnostics).containsExactly(diagnostic);
  }

  @Test
  void shouldWarnAboutMissingEventDto() {
    var event = givenEventWithPayload();
    var inputs = givenResolvedPayload();
    var diagnostics = new ArrayList<Diagnostic>();

    ((EventTool) getTool()).validate(event, inputs, diagnostics);

    assertThatSingleMissingEventDtoDiagnostic(diagnostics);
  }

  @Test
  void shouldWarnAboutMissingEventDtoWhenCodeIsInWrongPackage() {
    var event = givenEventWithPayload();
    var inputs = givenCodeInWrongPackage();
    var diagnostics = new ArrayList<Diagnostic>();

    ((EventTool) getTool()).validate(event, inputs, diagnostics);

    assertThatSingleMissingEventDtoDiagnostic(diagnostics);
  }

  @Test
  void shouldCreatePayloadForEvent() throws IOException {
    var workspace = workspaceFor("missing");

    var actual = validateAgainst(workspace);

    assertThatPayloadDiagnosticAndCreation(actual, workspace);
  }

  private void assertThatPayloadDiagnosticAndCreation(
      SequencedCollection<Diagnostic> diagnostics, Workspace<?> workspace) throws IOException {
    assertThat(diagnostics).as("Validation diagnostics").hasSize(1);
    var diagnostic = diagnostics.getFirst();
    assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
    assertThat(diagnostic.message()).as("Message").isEqualTo("Unknown entity 'Payload'");
    assertThat(diagnostic.location()).as("Location").hasToString("missing/event/WithPayload");
    assertThatCreatedPayload(diagnostic, workspace);
  }

  private void assertThatCreatedPayload(Diagnostic diagnostic, Workspace<?> workspace)
      throws IOException {
    assertThat(diagnostic.suggestions()).as("Suggestions").hasSize(1);
    var suggestion = diagnostic.suggestions().getFirst();
    assertThat(suggestion.message()).as("Suggestion").isEqualTo("Create entity");
    var created = apply(suggestion, diagnostic, workspace).createdOrChanged();
    var payload = workspace.root().select("src/main/design/Payload.entity");
    assertThat(created).as("Apply diagnostics").hasSize(1).contains(payload);
    assertThat(payload.readFrom()).hasContent(ENTITY_SKELETON);
  }

  private ResolvedInputs givenCodeInWrongPackage() {
    return givenResolvedPayload()
        .put(
            "codeArtifacts",
            List.of(
                new CodeArtifact(new FullyQualifiedName("wrong.package", "TodoItemAdded"))
                    .setCode("class TodoItemAdded {}")));
  }

  private Event givenEventWithPayload() {
    return new Event(new FullyQualifiedName("design", "TodoItemAdded"))
        .setPayload(new Link("entity", "Payload"));
  }

  private ResolvedInputs givenResolvedPayload() {
    return givenInputsWithAllPrerequisites()
        .put("entities", List.of(new Entity(new FullyQualifiedName("design", "Payload"))));
  }

  private void assertThatSingleMissingEventDtoDiagnostic(Collection<Diagnostic> diagnostics) {
    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            d -> {
              assertThat(d.level()).as("Level").isEqualTo(WARN);
              assertThat(d.message()).as("Message").isEqualTo("Missing event DTO");
              assertThat(d.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      s ->
                          assertThat(s.message()).as("Suggestion").isEqualTo("Generate event DTO"));
            });
  }
}
