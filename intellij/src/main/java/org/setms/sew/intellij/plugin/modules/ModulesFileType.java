package org.setms.sew.intellij.plugin.modules;

import org.setms.swe.inbound.tool.ModulesTool;
import org.setms.sew.intellij.plugin.filetype.SalLanguageFileType;
import org.setms.sew.intellij.plugin.filetype.SewIcons;

public class ModulesFileType extends SalLanguageFileType {

  public static final ModulesFileType INSTANCE = new ModulesFileType();

  private ModulesFileType() {
    super("Modules", SewIcons.MODULES, new ModulesTool());
  }
}
