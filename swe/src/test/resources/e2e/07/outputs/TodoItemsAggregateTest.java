package com.example.todo;

import static com.example.todo.TestDataBuilder.someAddTodoItem;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.todo.domain.model.TodoItemAdded;
import com.example.todo.domain.services.TodoItemsService;
import com.example.todo.domain.services.TodoItemsServiceImpl;
import org.junit.jupiter.api.Test;

class TodoItemsAggregateTest {

  private final TodoItemsService service = new TodoItemsServiceImpl();

  @Test
  void acceptAddTodoItemAndEmitTodoItemAdded() {
    var addTodoItem = someAddTodoItem();
    var expected = new TodoItemAdded(addTodoItem.getTask(), addTodoItem.getDueDate());

    var actual = service.accept(addTodoItem);

    assertThat(actual).isEqualTo(expected);
  }
}
