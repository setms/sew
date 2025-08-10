package org.setms.swe;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.kmsystem.KmSystem;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;

class EndToEndTest {

  private static final String DOMAIN_STORY_NAME = "AddTodo";
  private static final String DOMAIN_STORY_CONTENT =
      """
      package todo

      domainStory %s {
        description = "Add a todo item"
      }

      sentence {
        parts = [
          person(User),
          activity(Adds),
          workObject(TodoItem)
        ]
      }
      """;

  private final File root = new File("build/e2etest");
  private Workspace<?> workspace;
  private KmSystem kmSystem;

  @BeforeEach
  void init() {
    Files.delete(root);
    workspace = new DirectoryWorkspace(root);
  }

  @Test
  void shouldGuideSoftwareEngineering() throws IOException {
    assertThatExistingDomainStoryIsValidated();
  }

  private void assertThatExistingDomainStoryIsValidated() throws IOException {
    var domainStory =
        workspace
            .root()
            .select("src/main/requirements/%s.domainStory".formatted(DOMAIN_STORY_NAME));
    try (var writer = new PrintWriter(domainStory.writeTo())) {
      writer.println(DOMAIN_STORY_CONTENT.formatted(DOMAIN_STORY_NAME));
    }
    kmSystem = new KmSystem(workspace);
    await()
        .atMost(5, SECONDS)
        .untilAsserted(() -> assertThat(kmSystem.diagnosticsFor(domainStory.path())).isNotEmpty());
  }
}
