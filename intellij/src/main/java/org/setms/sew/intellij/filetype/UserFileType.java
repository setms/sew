package org.setms.sew.intellij.filetype;

import org.setms.sew.core.inbound.tool.StakeholdersTool;

public class UserFileType extends SalLanguageFileType {

  public static final UserFileType INSTANCE = new UserFileType();

  private UserFileType() {
    super("User", "Stakeholder who uses the system", SewIcons.USER, new StakeholdersTool());
  }
}
