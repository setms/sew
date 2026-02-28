package com.example.todo.domain.services;

import com.example.todo.domain.model.AddTodoItem;
import com.example.todo.domain.model.TodoItemAdded;

public interface TodoItemsService {

  TodoItemAdded accept(AddTodoItem addTodoItem);
}
