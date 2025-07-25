package org.setms.sew.core.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.INFO;
import static org.setms.km.domain.model.validation.Level.WARN;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Command;

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

  @Test
  void shouldCreatePayload() throws IOException {
    var workspace = workspaceFor("missing");

    var actual = getTool().validate(workspace);
    assertThat(actual).as("Validation diagnostics").hasSize(1);
    var diagnostic = actual.getFirst();
    assertThat(diagnostic.level()).as("Level").isEqualTo(WARN);
    assertThat(diagnostic.message()).as("Message").isEqualTo("Missing entity Payload");
    assertThat(diagnostic.location()).as("Location").hasToString("missing/command/WithPayload");
    assertThat(diagnostic.suggestions()).as("Suggestions").hasSize(1);
    var suggestion = diagnostic.suggestions().getFirst();
    assertThat(suggestion.message()).as("Suggestion").isEqualTo("Create entity");
    var diagnostics = getTool().apply(suggestion.code(), workspace, diagnostic.location());
    var payload = workspace.root().select("src/main/design/Payload.entity");
    assertThat(diagnostics)
        .as("Apply diagnostics")
        .hasSize(1)
        .contains(new Diagnostic(INFO, "Created " + payload.toUri()));
    try {
      assertThat(payload.readFrom()).hasContent(ENTITY_SKELETON);
    } finally {
      payload.delete();
    }
  }
}
