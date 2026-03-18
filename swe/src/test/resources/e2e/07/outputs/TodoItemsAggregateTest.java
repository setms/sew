package com.example.todo;

import static com.example.todo.TestDataBuilder.someAddTodoItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.example.todo.domain.model.TodoItem;
import com.example.todo.domain.model.TodoItemAdded;
import com.example.todo.domain.services.TodoItemsRepository;
import com.example.todo.domain.services.TodoItemsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TodoItemsAggregateTest {

  @InjectMocks private TodoItemsServiceImpl service;
  @Mock private TodoItemsRepository repository;

  @Test
  void acceptAddTodoItemAndEmitTodoItemAdded() {
    var addTodoItem = someAddTodoItem();
    var expected = new TodoItemAdded(addTodoItem.task(), addTodoItem.dueDate());
    var expectedTodoItem = new TodoItem(addTodoItem.task(), addTodoItem.dueDate());

    var actual = service.accept(addTodoItem);

    assertThat(actual).isEqualTo(expected);
    verify(repository).insert(expectedTodoItem);
  }
}
