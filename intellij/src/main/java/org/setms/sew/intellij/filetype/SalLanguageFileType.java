package org.setms.sew.intellij.filetype;

import static org.setms.sew.core.domain.model.format.Strings.initLower;

import javax.swing.Icon;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.intellij.lang.sal.SalLanguage;
import org.setms.sew.intellij.lang.sal.SalParserDefinition;

public abstract class SalLanguageFileType extends BaseLanguageFileType {

  protected SalLanguageFileType(
      String name, String description, String extension, Icon icon, Tool tool) {
    super(SalLanguage.INSTANCE, name, description, extension, icon, tool);
    SalParserDefinition.addFileType(this);
  }

  protected SalLanguageFileType(String name, String description, Icon icon, Tool tool) {
    this(name, description, initLower(name), icon, tool);
  }

  protected SalLanguageFileType(String name, Icon icon, Tool tool) {
    this(name, name, icon, tool);
  }
}
