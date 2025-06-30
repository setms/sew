package org.setms.sew.core.inbound.tool;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.setms.sew.core.domain.model.tool.Level.WARN;

import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.domain.model.format.Files;
import org.setms.sew.core.domain.model.format.Strings;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.eventstorming.Command;
import org.setms.sew.core.domain.model.sdlc.process.Todo;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Location;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Suggestion;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.format.sew.SewFormat;
import org.setms.sew.core.outbound.tool.file.FileInputSource;

class ToolTest {

  private static final String MESSAGE = "Some message";
  private static final String CODE = "Some code";
  private static final String SUGGESTION = "Some suggestion";

  private final Tool tool = new FakeTool();

  @Test
  void shouldCreateTodoForDiagnosticsWithSuggestions() throws IOException {
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
          new SewFormat().newParser().parse(new FileInputStream(todoFile), Todo.class, false);
      assertThat(todo).as("Invalid todo").isNotNull();
      assertThat(todo.getLocation()).as("Location").isEqualTo(diagnostic.location().toString());
      assertThat(todo.getMessage()).as("Message").isEqualTo(diagnostic.message());
      assertThat(todo.getCode()).as("Code").isEqualTo(diagnostic.suggestions().getFirst().code());
      assertThat(todo.getAction())
          .as("Action")
          .isEqualTo(diagnostic.suggestions().getFirst().message());
      assertThat(todo.getTool()).as("Tool").isEqualTo(FakeTool.class.getName());
    } finally {
      Files.delete(new File(baseDir, "src"));
    }
  }

  private static class FakeTool extends Tool {

    @Override
    public List<Input<?>> getInputs() {
      return List.of(new Input<>("bar", Command.class));
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
  public static class Foo extends NamedObject {

    @NotNull private String bar;

    public Foo(FullyQualifiedName fullyQualifiedName) {
      super(fullyQualifiedName);
    }
  }
}
