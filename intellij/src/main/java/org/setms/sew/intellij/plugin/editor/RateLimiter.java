package org.setms.sew.intellij.plugin.editor;

import javax.swing.Timer;

class RateLimiter {

  private final Timer timer;

  public RateLimiter(Runnable task, int delayMs) {
    timer = new Timer(delayMs, e -> task.run());
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
