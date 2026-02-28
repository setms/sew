package com.example.todo;

import static com.example.todo.TestDataBuilder.someAddTodoItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.todo.domain.model.TodoItemAdded;
import com.example.todo.domain.services.TodoItemsService;
import org.junit.jupiter.api.Test;

class TodoItemsAggregateTest {

  private final TodoItemsService service = new TodoItemsService();

  @Test
  void acceptAddTodoItemAndEmitTodoItemAdded() {
    var addTodoItem = someAddTodoItem();
    var expected =
        new TodoItemAdded().setTask(addTodoItem.getTask()).setDueDate(addTodoItem.getDueDate());

    var actual = service.accept(addTodoItem);

    assertThat(actual).isEqualTo(expected);
  }
}
