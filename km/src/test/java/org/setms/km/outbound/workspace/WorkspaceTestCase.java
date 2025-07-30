package org.setms.km.outbound.workspace;

import static java.lang.System.lineSeparator;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PROTECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.workspace.ArtifactDefinition;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.km.test.MainArtifact;
import org.setms.km.test.TestFormat;

public abstract class WorkspaceTestCase {

  private static final String CONTENT = "abc123";

  @Getter(PROTECTED)
  private Workspace workspace;

  @BeforeEach
  protected void init() {
    workspace = newWorkspace();
  }

  protected abstract Workspace newWorkspace();

  @AfterEach
  void done() throws IOException {
    workspace.root().delete();
  }

  @Test
  void shouldHaveRoot() {
    var root = workspace.root();

    assertThat(root.name()).as("Name").isEqualTo("");
    assertThat(root.path()).as("Path").isEqualTo("/");
    assertThat(root.parent()).as("Parent").isEmpty();
  }

  @Test
  void shouldHaveResourceTree() {
    var path = "ape/bear/cheetah";

    var resource = workspace.root().select(path);

    assertThat(resource.path()).as("Path").isEqualTo("/ape/bear/cheetah");
    assertThat(resource.name()).as("Name").isEqualTo("cheetah");
    assertThat(resource.parent())
        .as("Parent")
        .isPresent()
        .hasValueSatisfying(
            parent -> {
              assertThat(parent.path()).as("Parent path").isEqualTo("/ape/bear");
              assertThat(parent.name()).as("Parent name").isEqualTo("bear");
              assertThat(parent.parent())
                  .as("Grandparent")
                  .isPresent()
                  .hasValueSatisfying(
                      grandParent -> {
                        assertThat(grandParent.path()).as("Grandparent path").isEqualTo("/ape");
                        assertThat(grandParent.name()).as("Grandparent name").isEqualTo("ape");
                        assertThat(grandParent.parent())
                            .as("Great-grandparent")
                            .isPresent()
                            .hasValueSatisfying(
                                greatGrandparent -> {
                                  assertThat(greatGrandparent.path())
                                      .as("Great-grandparent path")
                                      .isEqualTo("/");
                                  assertThat(greatGrandparent.name())
                                      .as("Great-grandparent name")
                                      .isEqualTo("");
                                  assertThat(greatGrandparent.parent())
                                      .as("Great-grandparent parent")
                                      .isEmpty();
                                });
                      });
            });
    assertThat(resource.root()).as("Root").isEqualTo(workspace.root());
  }

  @Test
  void shouldNotEscapeRoot() {
    var resource = workspace.root().select("ape");

    assertThat(resource.select("../..")).isNull();
  }

  @Test
  void shouldNavigateDirectlyToResource() {
    var resource = workspace.root().select("ape");

    assertThat(resource.select("/bear/cheetah"))
        .isNotNull()
        .satisfies(selected -> assertThat(selected.path()).isEqualTo("/bear/cheetah"));
  }

  @Test
  void shouldFindChildren() throws IOException {
    var parent = workspace.root().select("parent");
    createChild(parent, "child1");
    createChild(parent, "child2");

    assertThat(parent.children())
        .as("# children")
        .hasSize(2)
        .map(Resource::name)
        .containsExactlyInAnyOrder("child1", "child2");
  }

  private Resource<? extends Resource<?>> createChild(
      Resource<? extends Resource<?>> parent, String child) throws IOException {
    var result = parent.select(child);
    try (var writer = new PrintWriter(result.writeTo())) {
      writer.println(CONTENT);
    }
    return result;
  }

  @Test
  void shouldStoreData() throws IOException {
    var resource = createChild(workspace.root(), "test");

    try (var reader = new BufferedReader(new InputStreamReader(resource.readFrom()))) {
      var content = reader.lines().collect(joining(lineSeparator()));
      assertThat(content).isEqualTo(CONTENT);
    }
  }

  @Test
  void shouldDeleteResource() throws IOException {
    createChild(workspace.root(), "bye").delete();

    assertThat(workspace.root().children()).isEmpty();
  }

  @Test
  void shouldFindByGlob() throws IOException {
    createChild(workspace.root(), "ape/bear/cheetah.dingo");
    createChild(workspace.root(), "ape/fox.dingo");
    createChild(workspace.root(), "elephant.dingo");
    createChild(workspace.root(), "fox/giraffe.dingo");
    createChild(workspace.root(), "ape/hyena.iguana");

    assertThat(workspace.root().matching(new Glob("ape", "**/*.dingo"))).hasSize(2);
  }

  @Test
  void shouldReportChangedArtifact() throws IOException {
    var format = new TestFormat();
    workspace.registerArtifactDefinition(
        new ArtifactDefinition(
            MainArtifact.class, new Glob("main", "**/*.mainArtifact"), format.newParser()));
    var artifact = new MainArtifact(new FullyQualifiedName("ape.Bear"));
    var path = "/main/ape/Bear.mainArtifact";
    var changed = new AtomicBoolean();
    workspace.registerArtifactChangedHandler(
        (changedPath, changedArtifact) -> {
          changed.set(true);
          assertThat(changedArtifact).as("Changed artifact").isEqualTo(artifact);
          assertThat(changedPath).as("Changed path").isEqualTo(path);
        });

    var resource = workspace.root().select(path);
    try (var output = resource.writeTo()) {
      format.newBuilder().build(artifact, output);
    }

    await()
        .atMost(250, MILLISECONDS)
        .untilAsserted(() -> assertThat(changed.get()).as("Changed handler called").isTrue());
  }
}
