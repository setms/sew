package com.example.todo.domain.model;

import java.time.LocalDateTime;

public record AddTodoItem(String task, LocalDateTime dueDate) {}
