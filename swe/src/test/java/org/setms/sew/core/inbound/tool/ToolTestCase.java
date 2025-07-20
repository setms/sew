package org.setms.sew.core.inbound.tool;

import static lombok.AccessLevel.PROTECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.km.domain.model.format.Strings.initLower;

import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Files;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Tool;
import org.setms.km.domain.model.workspace.InputSource;
import org.setms.km.outbound.workspace.file.FileInputSource;
import org.setms.km.outbound.workspace.file.FileOutputSink;
import org.setms.sew.core.inbound.format.sal.SalFormat;

abstract class ToolTestCase<T extends Artifact> {

  @Getter(PROTECTED)
  private final Tool tool;

  private final Class<? extends Format> formatType;
  private final String sourceLocation;
  private final String extension;
  private final File baseDir;

  protected ToolTestCase(Tool tool, Class<T> type, String sourceLocation) {
    this(tool, type, SalFormat.class, sourceLocation);
  }

  protected ToolTestCase(
      Tool tool, Class<T> type, Class<? extends Format> formatType, String sourceLocation) {
    this.tool = tool;
    this.formatType = formatType;
    this.sourceLocation = sourceLocation;
    this.extension = initLower(type.getSimpleName());
    this.baseDir = new File("src/test/resources/" + extension);
  }

  @AfterEach
  void done() {
    Files.childrenOf(baseDir).forEach(this::deleteTodos);
  }

  private void deleteTodos(File dir) {
    var todoDir = new File(dir, "src/todo");
    if (todoDir.exists()) {
      Files.delete(todoDir);
    } else {
      Files.childrenOf(dir).forEach(this::deleteTodos);
    }
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
    var testDir = getTestDir("valid");
    var input = (Input<T>) tool.getInputs().getFirst();
    var matchingObjects = new FileInputSource(testDir).matching(input.glob());
    assertThat(matchingObjects).as("Missing objects at " + testDir).isNotEmpty();
    var source = matchingObjects.iterator().next();

    try (var sutStream = source.open()) {
      var parsed = input.format().newParser().parse(sutStream, input.type(), true);

      assertThat(parsed).isNotNull();
      assertThatParsedObjectMatchesExpectations(parsed);
    }
  }

  protected File getTestDir(String name) {
    return new File(baseDir, name);
  }

  protected void assertThatParsedObjectMatchesExpectations(T parsed) {
    // Override to add assertions
  }

  protected InputSource inputSourceFor(String path) {
    return new FileInputSource(getTestDir(path));
  }

  @Test
  void shouldBuild() throws IOException {
    var testDir = getTestDir("valid");
    var sink = new FileOutputSink(new File(testDir, "build"));
    var diagnostics = tool.build(new FileInputSource(testDir), sink);
    assertThat(diagnostics).as("Diagnostics").isEmpty();
    assertBuild(sink);
  }

  protected void assertBuild(FileOutputSink sink) {
    // Override to add assertions
  }
}
