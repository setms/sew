package com.example.todo.domain.services;

import com.example.todo.domain.model.TodoItems;
import java.util.Collection;

public interface TodoItemsRepository {

  Collection<TodoItems> loadAll();

  void insert(TodoItems aggregate);

  void update(TodoItems aggregate);
}
