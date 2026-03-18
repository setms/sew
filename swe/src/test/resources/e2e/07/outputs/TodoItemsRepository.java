package com.example.todo.domain.services;

import com.example.todo.domain.model.TodoItem;
import java.util.Collection;

public interface TodoItemsRepository {

  Collection<TodoItem> loadAll();

  void insert(TodoItem aggregate);

  void update(TodoItem aggregate);
}
