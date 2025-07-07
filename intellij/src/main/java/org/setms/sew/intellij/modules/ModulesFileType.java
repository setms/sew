package org.setms.sew.intellij.modules;

import org.setms.sew.core.inbound.tool.ModulesTool;
import org.setms.sew.intellij.filetype.SalLanguageFileType;
import org.setms.sew.intellij.filetype.SewIcons;

public class ModulesFileType extends SalLanguageFileType {

  public static final ModulesFileType INSTANCE = new ModulesFileType();

  private ModulesFileType() {
    super("Modules", SewIcons.MODULES, new ModulesTool());
  }
}
