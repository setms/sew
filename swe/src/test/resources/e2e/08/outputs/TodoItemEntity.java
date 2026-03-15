package com.example.todo.outbound.db;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class TodoItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;
  @Column("id")
  private UUID iD;

  @Column("task")
  private String task;

  @Column("due_date")
  private LocalDateTime dueDate
}
