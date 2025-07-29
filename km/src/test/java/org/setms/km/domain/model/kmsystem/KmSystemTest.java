package org.setms.km.domain.model.kmsystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.validation.Level.ERROR;

import java.io.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.*;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.*;

class KmSystemTest {

  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private KmSystem kmSystem;

  private final Workspace workspace = new InMemoryWorkspace();
  private final MainTool mainTool = new MainTool();
  private final OtherTool otherTool = new OtherTool();

  @BeforeEach
  void init() {
    Tools.reload();
    Tools.add(mainTool);
    Tools.add(otherTool);
  }

  @Test
  void shouldNotBuildInvalidChangedArtifact() throws IOException {
    createKmSystem();
    MainTool.init(new Diagnostic(ERROR, "message"));
    OtherTool.init();

    storeNewMainArtifact();

    assertThat(MainTool.validated).as("main validated").isTrue();
    assertThat(MainTool.built).as("main built").isFalse();
    assertThat(OtherTool.validated).as("other validated").isFalse();
    assertThat(OtherTool.built).as("other built").isFalse();
  }

  private void createKmSystem() {
    kmSystem = new KmSystem((workspace));
  }

  private String storeNewMainArtifact() throws IOException {
    var glob = mainTool.getInputs().getFirst().glob();
    var resource =
        workspace
            .root()
            .select(glob.path())
            .select("Bear." + glob.pattern().substring(1 + glob.pattern().lastIndexOf('.')));
    try (var output = resource.writeTo()) {
      new TestFormat()
          .newBuilder()
          .build(new MainArtifact(new FullyQualifiedName("ape.Bear")), output);
    }
    return resource.path();
  }

  @Test
  void shouldBuildValidChangedArtifactAndDependents() throws IOException {
    createKmSystem();
    MainTool.init();
    OtherTool.init();

    storeNewMainArtifact();

    assertThat(MainTool.validated).as("main validated").isTrue();
    assertThat(MainTool.built).as("main built").isTrue();
    assertThat(OtherTool.validated).as("other validated").isFalse();
    assertThat(OtherTool.built).as("other built").isTrue();
  }

  @Test
  void shouldUpdateCachedGlobsWhenMatchingArtifactCreated() throws IOException {
    createKmSystem();

    var path = storeNewMainArtifact();

    assertThat(globsForMainTool()).hasSize(1).containsExactlyInAnyOrder(path);
  }

  private List<String> globsForMainTool() throws IOException {
    var globPaths =
        workspace
            .root()
            .select(".km/globs/%s.glob".formatted(mainTool.getInputs().getFirst().glob()));
    try (var reader = new BufferedReader(new InputStreamReader(globPaths.readFrom()))) {
      return reader.lines().toList();
    }
  }

  @Test
  void shouldUpdateCachedGlobsWhenMatchingArtifactDeleted() throws IOException {
    var path = storeNewMainArtifact();
    createKmSystem();
    assertThat(globsForMainTool()).hasSize(1);

    workspace.root().select(path).delete();

    assertThat(globsForMainTool()).isEmpty();
  }
}
