package org.setms.sew.intellij.filetype;

public class ClockEventFileType extends SewLanguageFileType {

  public static final ClockEventFileType INSTANCE = new ClockEventFileType();

  private ClockEventFileType() {
    super(
        "ClockEvent", "Event that's triggered by the passing of time", SewIcons.CLOCK_EVENT, null);
  }
}
