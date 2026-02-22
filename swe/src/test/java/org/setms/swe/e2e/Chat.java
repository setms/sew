package org.setms.swe.e2e;

import static org.setms.km.domain.model.format.Strings.NL;

public class Chat {

  private static final int LINE_LENGTH = 130;
  private static final String TOPIC_SEPARATOR = "━";
  private static final String START = "> ";
  private static final String END = " <";

  private boolean lastSpoken = false;
  private final String firstActor;
  private final String secondActor;

  public Chat(String first, String second) {
    firstActor = first;
    secondActor = second;
  }

  public void topic(String topic) {
    var prefix = TOPIC_SEPARATOR.repeat((LINE_LENGTH - topic.length()) / 2 - 1);
    var suffix = TOPIC_SEPARATOR.repeat(LINE_LENGTH - topic.length() - prefix.length() - 2);
    print(NL + prefix + " " + topic + " " + suffix + NL, true);
  }

  private void print(String text, boolean alignLeft) {
    var value = alignLeft ? text : " ".repeat(LINE_LENGTH - text.length()) + text;
    System.out.println(value);
  }

  public void add(boolean first, String text) {
    if (first != lastSpoken) {
      lastSpoken = first;
      print(first ? firstActor : secondActor, first);
    }
    print(first ? START + text : text + END, first);
  }
}
