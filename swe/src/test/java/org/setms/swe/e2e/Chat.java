package org.setms.swe.e2e;

import static org.setms.km.domain.model.format.Strings.NL;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Chat implements Closeable {

  private static final int LINE_LENGTH = 130;
  private static final String TOPIC_SEPARATOR = "━";
  private static final String START = "> ";
  private static final String END = " <";

  private boolean lastSpoken = false;
  private final String firstActor;
  private final String secondActor;
  private final List<Consumer<String>> writers;

  public void topic(String topic) {
    var prefix = TOPIC_SEPARATOR.repeat((LINE_LENGTH - topic.length()) / 2 - 1);
    var suffix = TOPIC_SEPARATOR.repeat(LINE_LENGTH - topic.length() - prefix.length() - 2);
    print(NL + prefix + " " + topic + " " + suffix + NL, true);
  }

  private void print(String text, boolean alignLeft) {
    var value = alignLeft ? text : " ".repeat(LINE_LENGTH - text.length()) + text;
    writers.forEach(writer -> writer.accept(value));
  }

  public void add(boolean first, String text) {
    if (first != lastSpoken) {
      lastSpoken = first;
      print(first ? firstActor : secondActor, first);
    }
    print(first ? START + text : text + END, first);
  }

  @Override
  public void close() {
    writers.stream()
        .filter(Closeable.class::isInstance)
        .map(Closeable.class::cast)
        .forEach(
            closeable -> {
              try {
                closeable.close();
              } catch (IOException ignored) {
                // Ignore
              }
            });
  }
}
