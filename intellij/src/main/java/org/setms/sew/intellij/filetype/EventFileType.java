package org.setms.sew.intellij.filetype;

public class EventFileType extends SewLanguageFileType {

  public static final EventFileType INSTANCE = new EventFileType();

  private EventFileType() {
    super(
        "Event",
        "Something that happens that's interesting from a business perspective",
        SewIcons.EVENT,
        null);
  }
}
