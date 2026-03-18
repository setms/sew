package com.example.todo.domain.services;

import com.example.todo.domain.model.AddTodoItem;
import com.example.todo.domain.model.TodoItem;
import com.example.todo.domain.model.TodoItemAdded;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TodoItemsServiceImpl implements TodoItemsService {

  private final TodoItemsRepository repository;

  @Override
  public TodoItemAdded accept(AddTodoItem addTodoItem) {
    var newTodoItem = new TodoItem(addTodoItem.task(), addTodoItem.dueDate());
    repository.insert(newTodoItem);
    return new TodoItemAdded(newTodoItem.task(), newTodoItem.dueDate());
  }
}
