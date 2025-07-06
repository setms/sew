package org.setms.sew.intellij.filetype;

public class CommandFileType extends SewLanguageFileType {

  public static final CommandFileType INSTANCE = new CommandFileType();

  private CommandFileType() {
    super(
        "Command",
        "Command instructing an aggregate or external system to do something",
        SewIcons.COMMAND,
        null);
  }
}
