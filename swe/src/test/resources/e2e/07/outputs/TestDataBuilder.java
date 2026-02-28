package com.example.todo;

import com.example.todo.domain.model.AddTodoItem;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestDataBuilder {

  public static AddTodoItem someAddTodoItem() {
    return addTodoItems().sample();
  }

  private static Arbitrary<AddTodoItem> addTodoItems() {
    return Combinators.combine(tasks(), dueDates()).as(AddTodoItem::new);
  }

  private static Arbitrary<String> tasks() {
    return Arbitraries.strings().ofMinLength(1);
  }

  private static Arbitrary<LocalDateTime> dueDates() {
    return Arbitraries.defaultFor(LocalDateTime.class);
  }
}
