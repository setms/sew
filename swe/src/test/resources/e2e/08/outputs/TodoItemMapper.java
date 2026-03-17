package com.example.todo.outbound.db;

import com.example.todo.domain.model.TodoItems;
import org.mapstruct.Mapper;

@Mapper
public interface TodoItemMapper {

  TodoItems toAggregate(TodoItemEntity entity);

  TodoItemEntity toEntity(TodoItems aggregate);
}
