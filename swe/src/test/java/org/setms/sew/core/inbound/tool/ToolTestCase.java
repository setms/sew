package org.setms.sew.core.inbound.tool;

import static lombok.AccessLevel.PROTECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.format.Strings.initLower;

import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;
import org.setms.sew.core.inbound.format.sal.SalFormat;

abstract class ToolTestCase<T extends Artifact> {

  private static final String FILE_URI_SCHEME = "file:";

  @Getter(PROTECTED)
  private final BaseTool tool;

  private final Class<? extends Format> formatType;
  private final String sourceLocation;
  private final String extension;
  private final File baseDir;

  protected ToolTestCase(BaseTool tool, Class<T> type, String sourceLocation) {
    this(tool, type, SalFormat.class, sourceLocation);
  }

  protected ToolTestCase(
      BaseTool tool, Class<T> type, Class<? extends Format> formatType, String sourceLocation) {
    this.tool = tool;
    this.formatType = formatType;
    this.sourceLocation = sourceLocation;
    this.extension = initLower(type.getSimpleName());
    this.baseDir = new File("src/test/resources/" + extension);
  }

  @Test
  void shouldDefineInputs() {
    var actual = tool.getInputs();

    assertThat(actual).isNotEmpty();
    assertInputs(actual);
  }

  protected void assertInputs(List<Input<?>> actual) {
    var input = actual.getFirst();
    assertThat(input.glob()).hasToString("src/%s/**/*.%s".formatted(sourceLocation, extension));
    assertThat(input.format()).isInstanceOf(formatType);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldParseObject() throws IOException {
    var workspace = workspaceFor("valid");
    var input = (Input<T>) tool.getInputs().getFirst();
    var matchingObjects = workspace.root().matching(input.glob());
    assertThat(matchingObjects).as("Missing objects at").isNotEmpty();
    for (var source : matchingObjects) {
      try (var sutStream = source.readFrom()) {
        var parsed = input.format().newParser().parse(sutStream, input.type(), true);

        assertThat(parsed).isNotNull();
        assertThatParsedObjectMatchesExpectations(parsed);
      }
    }
  }

  private File getTestDir(String name) {
    return new File(baseDir, name);
  }

  protected void assertThatParsedObjectMatchesExpectations(T parsed) {
    // Override to add assertions
  }

  protected Workspace workspaceFor(String path) {
    return new DirectoryWorkspace(getTestDir(path));
  }

  @Test
  void shouldBuild() {
    var workspace = workspaceFor("valid");
    var resource = workspace.root();
    var diagnostics = tool.build(workspace);
    assertThat(diagnostics).as("Diagnostics").isEmpty();
    assertBuild(resource);
  }

  protected void assertBuild(Resource<?> resource) {
    // Override to add assertions
  }

  protected File toFile(Resource<?> resource) {
    var path = resource.toUri().toString();
    if (path.startsWith(FILE_URI_SCHEME)) {
      path = path.substring(FILE_URI_SCHEME.length());
    }
    return new File(path);
  }
}
