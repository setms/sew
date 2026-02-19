package com.company.todo;

import static com.company.todo.TestData.someAddTodoItem;
import static com.company.todo.domain.model.TodoItemAdded;
import static com.company.todo.domain.services.TodoItemsService;
import static org.assertj.core.api.Assertions.assertThat;

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
