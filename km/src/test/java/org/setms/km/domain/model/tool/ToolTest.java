package org.setms.km.domain.model.tool;


import java.io.IOException;
import org.junit.jupiter.api.Test;

class ToolTest {

  private final Tool tool = new FooTool();

  @Test
  void shouldCreateTodoForDiagnosticsWithSuggestions() throws IOException {
    /* TODO: Move to collaboration package
    var baseDir = new File("src/test/resources/foo");
    var diagnostics = tool.validate(new FileInputSource(baseDir));

    assertThat(diagnostics).hasSize(1);
    var diagnostic = diagnostics.getFirst();
    var todoFile =
        new File(
            baseDir,
            "src/todo/%s/%s.todo"
                .formatted(diagnostic.location(), Strings.toObjectName(diagnostic.message())));
    assertThat(todoFile).isFile();
    try {
      var todo =
          new SalFormat().newParser().parse(new FileInputStream(todoFile), Todo.class, false);
      Assertions.assertThat(todo).as("Invalid todo").isNotNull();
      Assertions.assertThat(todo.getLocation())
          .as("Location")
          .isEqualTo(diagnostic.location().toString());
      Assertions.assertThat(todo.getMessage()).as("Message").isEqualTo(diagnostic.message());
      Assertions.assertThat(todo.getCode())
          .as("Code")
          .isEqualTo(diagnostic.suggestions().getFirst().code());
      Assertions.assertThat(todo.getAction())
          .as("Action")
          .isEqualTo(diagnostic.suggestions().getFirst().message());
      Assertions.assertThat(todo.getTool()).as("Tool").isEqualTo(FakeTool.class.getName());
    } finally {
      Files.delete(new File(baseDir, "src"));
    }
     */
  }
}
