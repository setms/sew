package com.example.todo.outbound.db;

import com.example.todo.domain.model.TodoItem;
import org.mapstruct.Mapper;

@Mapper
public interface TodoItemMapper {

  TodoItem toAggregate(TodoItemEntity entity);

  TodoItemEntity toEntity(TodoItem aggregate);
}
