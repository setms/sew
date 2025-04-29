package org.setms.sew.intellij;

import javax.swing.*;

class Debouncer {

  private final Timer timer;

  public Debouncer(Runnable task, long delayMs) {
    timer = new Timer((int) delayMs, e -> task.run());
    timer.setRepeats(false);
  }

  public void call() {
    if (timer.isRunning()) {
      timer.restart();
    } else {
      timer.start();
    }
  }

  public void cancel() {
    timer.stop();
  }
}
