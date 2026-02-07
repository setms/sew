package todo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TodoItemsTest {

  @Test
  void acceptAddTodoItemAndEmitTodoItemAdded() {
    var task = TestData.someText();
    var dueDate = TestData.someDateTime();
    var addTodoItem = new AddTodoItem().setTask(task).setDueDate(dueDate);

    var actual = new TodoItems().accept(addTodoItem);

    assertThat(actual).isEqualTo(new TodoItemAdded().setTask(task).setDueDate(dueDate));
  }
}
