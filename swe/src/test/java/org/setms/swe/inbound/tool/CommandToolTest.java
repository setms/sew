package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.setms.km.outbound.workspace.memory.InMemoryWorkspace;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.ProgrammingLanguage;
import org.setms.swe.domain.model.sdlc.architecture.TopLevelPackage;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.overview.Initiative;

class CommandToolTest extends ToolTestCase<Command> {

  private static final String ENTITY_SKELETON =
      """
    package missing

    entity Payload {
    }
    """;

  CommandToolTest() {
    super(new CommandTool(), Command.class, "main/design");
  }

  @AfterEach
  void cleanupGeneratedFiles() throws IOException {
    workspaceFor("missing").root().select("src/main/design/Payload.entity").delete();
  }

  @Test
  void shouldNotWarnAboutMissingCodeWhenCommandHasNoPayload() {
    var command =
        new Command(new FullyQualifiedName("design", "WithoutPayload")).setDisplay("Do It");
    var diagnostics = new ArrayList<Diagnostic>();

    ((CommandTool) getTool()).validate(command, new ResolvedInputs(), diagnostics);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void shouldWarnAboutMissingCodeWhenCodeIsInWrongPackage() {
    var command = givenCommandWithPayload();
    var inputs = givenCodeInWrongPackage();
    var diagnostics = new ArrayList<Diagnostic>();

    ((CommandTool) getTool()).validate(command, inputs, diagnostics);

    assertThatSingleMissingCodeDiagnostic(diagnostics);
  }

  private Command givenCommandWithPayload() {
    return new Command(new FullyQualifiedName("design", "WithPayload"))
        .setDisplay("Do It")
        .setPayload(new Link("entity", "Payload"));
  }

  private ResolvedInputs givenCodeInWrongPackage() {
    return givenResolvedPayload()
        .put(
            "codeArtifacts",
            List.of(
                new CodeArtifact(new FullyQualifiedName("wrong.package", "WithPayload"))
                    .setCode("class WithPayload {}")));
  }

  @Test
  void shouldWarnAboutMissingCode() {
    var command = givenCommandWithPayload();
    var inputs = givenResolvedPayload();
    var diagnostics = new ArrayList<Diagnostic>();

    ((CommandTool) getTool()).validate(command, inputs, diagnostics);

    assertThatSingleMissingCodeDiagnostic(diagnostics);
  }

  private ResolvedInputs givenResolvedPayload() {
    return new ResolvedInputs()
        .put("entities", List.of(new Entity(new FullyQualifiedName("design", "Payload"))));
  }

  private void assertThatSingleMissingCodeDiagnostic(Collection<Diagnostic> diagnostics) {
    assertThat(diagnostics)
        .hasSize(1)
        .allSatisfy(
            d -> {
              assertThat(d.level()).as("Level").isEqualTo(WARN);
              assertThat(d.message()).as("Message").isEqualTo("Missing code");
              assertThat(d.suggestions())
                  .hasSize(1)
                  .allSatisfy(
                      s -> assertThat(s.message()).as("Suggestion").isEqualTo("Generate code"));
            });
  }

  @Test
  void shouldGenerateCodeForCommand() {
    var command =
        new Command(new FullyQualifiedName("design", "WithoutPayload")).setDisplay("Do It");
    var inputs = givenInputsWithAllPrerequisites();
    var workspace = new InMemoryWorkspace();

    var actual =
        ((CommandTool) getTool())
            .applySuggestion(command, CommandTool.GENERATE_CODE, null, inputs, workspace.root());

    assertThat(actual.createdOrChanged()).as("Created artifacts").isNotEmpty();
  }

  private ResolvedInputs givenInputsWithAllPrerequisites() {
    return new ResolvedInputs()
        .put(
            "initiatives",
            List.of(
                new Initiative(new FullyQualifiedName("overview", "Project"))
                    .setOrganization("Example")
                    .setTitle("Project")))
        .put(
            "decisions",
            List.of(
                new Decision(new FullyQualifiedName("technology", "ProgrammingLanguage"))
                    .setTopic(ProgrammingLanguage.TOPIC)
                    .setChoice("Java"),
                new Decision(new FullyQualifiedName("technology", "TopLevelPackage"))
                    .setTopic(TopLevelPackage.TOPIC)
                    .setChoice("com.example")));
  }

  @Test
  void shouldCreatePayload() throws IOException {
    var workspace = workspaceFor("missing");

    var actual = validateAgainst(workspace);

    assertThatPayloadDiagnosticAndCreation(actual, workspace);
  }

  private void assertThatPayloadDiagnosticAndCreation(
      SequencedCollection<Diagnostic> diagnostics, Workspace<?> workspace) throws IOException {
    assertThat(diagnostics).as("Validation diagnostics").hasSize(1);
    var diagnostic = diagnostics.getFirst();
    assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
    assertThat(diagnostic.message()).as("Message").isEqualTo("Missing entity Payload");
    assertThat(diagnostic.location()).as("Location").hasToString("missing/command/WithPayload");
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
}
