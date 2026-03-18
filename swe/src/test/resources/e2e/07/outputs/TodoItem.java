package com.example.todo.domain.model;

import java.time.LocalDateTime;

public record TodoItem(String task, LocalDateTime dueDate) {}
