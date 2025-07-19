package org.setms.km.domain.tool;

import static java.util.Collections.emptyList;
import static org.setms.km.domain.model.validation.Level.WARN;

import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Output;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.Suggestion;
import org.setms.km.domain.model.tool.Tool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;

class ToolTest {

  private static final String MESSAGE = "Some message";
  private static final String CODE = "Some code";
  private static final String SUGGESTION = "Some suggestion";

  private final Tool tool = new FakeTool();

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

  private static class FakeTool extends Tool {

    @Override
    public List<Input<?>> getInputs() {
      return List.of(/* new Input<>("bar", new SalFormat(), Command.class) */);
    }

    @Override
    public List<Output> getOutputs() {
      return emptyList();
    }

    @Override
    protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              MESSAGE,
              new Location("foo", "Baz"),
              List.of(new Suggestion(CODE, SUGGESTION))));
    }
  }

  @Getter
  @Setter
  @Accessors(chain = true)
  @ToString(callSuper = true)
  @EqualsAndHashCode(callSuper = true)
  public static class Foo extends Artifact {

    @NotNull private String bar;

    public Foo(FullyQualifiedName fullyQualifiedName) {
      super(fullyQualifiedName);
    }
  }
}
