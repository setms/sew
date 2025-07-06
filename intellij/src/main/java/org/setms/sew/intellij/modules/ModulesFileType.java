package org.setms.sew.intellij.modules;

import org.setms.sew.core.inbound.tool.ModulesTool;
import org.setms.sew.intellij.filetype.SewIcons;
import org.setms.sew.intellij.filetype.SewLanguageFileType;

public class ModulesFileType extends SewLanguageFileType {

  public static final ModulesFileType INSTANCE = new ModulesFileType();

  private ModulesFileType() {
    super("Modules", SewIcons.MODULES, new ModulesTool());
  }
}
