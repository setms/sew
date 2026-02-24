package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;

class CommandToolTest extends ToolTestCase<Command> {

  private final CommandTool commandTool = new CommandTool();

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
  void shouldWarnAboutMissingCode() {
    var command =
        new Command(new FullyQualifiedName("design", "WithoutPayload")).setDisplay("Do It");
    var diagnostics = new ArrayList<Diagnostic>();

    commandTool.validate(command, new ResolvedInputs(), diagnostics);

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
  void shouldCreatePayload() throws IOException {
    var workspace = workspaceFor("missing");

    var actual = validateAgainst(workspace);

    assertThat(actual).as("Validation diagnostics").hasSize(1);
    var diagnostic = actual.getFirst();
    assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
    assertThat(diagnostic.message()).as("Message").isEqualTo("Missing entity Payload");
    assertThat(diagnostic.location()).as("Location").hasToString("missing/command/WithPayload");
    assertThat(diagnostic.suggestions()).as("Suggestions").hasSize(1);
    var suggestion = diagnostic.suggestions().getFirst();
    assertThat(suggestion.message()).as("Suggestion").isEqualTo("Create entity");
    var created = apply(suggestion, diagnostic, workspace).createdOrChanged();
    var payload = workspace.root().select("src/main/design/Payload.entity");
    assertThat(created).as("Apply diagnostics").hasSize(1).contains(payload);
    assertThat(payload.readFrom()).hasContent(ENTITY_SKELETON);
  }
}
