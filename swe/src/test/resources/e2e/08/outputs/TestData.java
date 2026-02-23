package com.example.todo;

import com.example.todo.domain.model.AddTodoItem;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestData {

  public static AddTodoItem someAddTodoItem() {
    return addTodoItems().sample();
  }

  private static Arbitrary<AddTodoItem> addTodoItems() {
    return Combinators.combine(tasks(), dueDates()).as(AddTodoItem::new);
  }

  private static Arbitrary<Text> tasks() {
    return Arbitraries.strings().ofMinLength(1).map(Text::new);
  }

  private static Arbitrary<OffsetDateTime> dueDates() {
    return Arbitraries.defaultFor(OffsetDateTime.class);
  }
}
