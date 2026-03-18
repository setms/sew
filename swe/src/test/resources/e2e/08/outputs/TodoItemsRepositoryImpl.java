package com.example.todo.outbound.db;

import com.example.todo.domain.model.TodoItem;
import com.example.todo.domain.services.TodoItemRepository;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TodoItemRepositoryImpl implements TodoItemRepository {

  private final TodoItemJpaRepository jpaRepository;
  private final TodoItemMapper mapper;

  @Override
  public Collection<TodoItem> loadAll() {
    return jpaRepository.findAll().stream().map(mapper::toAggregate).toList();
  }

  @Override
  public void insert(TodoItem aggregate) {
    jpaRepository.save(mapper.toEntity(aggregate));
  }

  @Override
  public void update(TodoItem aggregate) {
    jpaRepository.save(mapper.toEntity(aggregate));
  }
}
