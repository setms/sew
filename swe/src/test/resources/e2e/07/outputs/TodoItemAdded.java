package com.example.todo.domain.model;

import java.time.LocalDateTime;

public record TodoItemAdded(String task, LocalDateTime dueDate) {}
