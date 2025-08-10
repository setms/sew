package org.setms.sew.intellij.plugin.filetype;

import org.setms.swe.inbound.tool.CommandTool;

public class CommandFileType extends SalLanguageFileType {

  public static final CommandFileType INSTANCE = new CommandFileType();

  private CommandFileType() {
    super(
        "Command",
        "Command instructing an aggregate or external system to do something",
        SewIcons.COMMAND,
        new CommandTool());
  }
}
