package org.setms.sew.intellij.plugin.filetype;

import org.setms.swe.inbound.tool.ProjectTool;

public class UserFileType extends SalLanguageFileType {

  public static final UserFileType INSTANCE = new UserFileType();

  private UserFileType() {
    super("User", "Stakeholder who uses the system", SewIcons.USER, new ProjectTool());
  }
}
