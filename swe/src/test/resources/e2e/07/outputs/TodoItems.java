package com.example.todo.domain.model;

import java.time.LocalDateTime;

public record TodoItems(String task, LocalDateTime dueDate) {}
