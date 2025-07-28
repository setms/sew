package org.setms.km.outbound.workspace.dir;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.format.Parser;
import org.setms.km.domain.model.format.RootObject;
import org.setms.km.domain.model.workspace.ArtifactDefinition;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Workspace;

class DirectoryWorkspaceTest {

  private final Parser parser = new TextParser();
  private final File file = new File("build/directory-workspace");
  private Workspace workspace;
  private boolean changed;

  @BeforeEach
  void init() {
    Files.delete(file);
    workspace = new DirectoryWorkspace(file);
  }

  @Test
  void shouldReportArtifactChanges() throws FileNotFoundException {
    workspace.registerArtifactDefinition(
        new ArtifactDefinition(Text.class, new Glob("text", "**/*.txt"), parser));
    workspace.registerArtifactChangedHandler(this::artifactChanged);

    addTextFile();

    await()
        .atMost(1, SECONDS)
        .untilAsserted(() -> assertThat(changed).as("Change reported").isTrue());
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void addTextFile() throws FileNotFoundException {
    var textFile = new File(file, "text/test.txt");
    textFile.getParentFile().mkdirs();
    try (var writer = new PrintWriter(textFile)) {
      writer.println("test");
    }
  }

  private void artifactChanged(Artifact artifact) {
    assertThat(artifact).isInstanceOf(Text.class);
    changed = true;
  }

  public static class Text extends Artifact {

    public Text(FullyQualifiedName fullyQualifiedName) {
      super(fullyQualifiedName);
    }
  }

  public static class TextParser implements Parser {

    @Override
    public RootObject parse(InputStream input) {
      return new RootObject("ape", Text.class.getSimpleName(), "Bear");
    }
  }
}
