package com.example.todo.domain.services;

import com.example.todo.domain.model.AddTodoItem;
import com.example.todo.domain.model.TodoItemAdded;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TodoItemsServiceImpl implements TodoItemsService {

  private final TodoItemsRepository repository;

  @Override
  public TodoItemAdded accept(AddTodoItem addTodoItem) {
    return new TodoItemAdded(addTodoItem.task(), addTodoItem.dueDate());
  }
}
