package com.example.todo.inbound.http;

import com.example.todo.domain.model.AddTodoItem;
import com.example.todo.domain.model.TodoItemAdded;
import com.example.todo.domain.services.TodoItemsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TodoItemsController {

  private final TodoItemsService todoItemsService;

  @PostMapping("/add-todo-item")
  public TodoItemAdded addTodoItem(@RequestBody AddTodoItem addTodoItem) {
    return todoItemsService.accept(addTodoItem);
  }
}
