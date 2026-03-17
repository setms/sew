package com.example.todo.outbound.db;

import com.example.todo.domain.model.TodoItems;
import com.example.todo.domain.services.TodoItemsRepository;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TodoItemsRepositoryImpl implements TodoItemsRepository {

  private final TodoItemJpaRepository jpaRepository;
  private final TodoItemMapper mapper;

  @Override
  public Collection<TodoItems> loadAll() {
    return jpaRepository.findAll().stream().map(mapper::toAggregate).toList();
  }

  @Override
  public void insert(TodoItems aggregate) {
    jpaRepository.save(mapper.toEntity(aggregate));
  }

  @Override
  public void update(TodoItems aggregate) {
    jpaRepository.save(mapper.toEntity(aggregate));
  }
}
