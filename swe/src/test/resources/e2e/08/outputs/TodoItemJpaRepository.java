package com.example.todo.outbound.db;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoItemJpaRepository extends JpaRepository<TodoItemEntity, UUID> {}
