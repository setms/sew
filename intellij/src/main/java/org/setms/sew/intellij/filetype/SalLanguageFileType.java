package org.setms.sew.intellij.filetype;

import static org.setms.km.domain.model.format.Strings.initLower;

import javax.swing.Icon;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.sew.intellij.lang.sal.SalLanguage;
import org.setms.sew.intellij.lang.sal.SalParserDefinition;

public abstract class SalLanguageFileType extends BaseLanguageFileType {

  protected SalLanguageFileType(
      String name, String description, String extension, Icon icon, BaseTool tool) {
    super(SalLanguage.INSTANCE, name, description, extension, icon, tool);
    SalParserDefinition.addFileType(this);
  }

  protected SalLanguageFileType(String name, String description, Icon icon, BaseTool tool) {
    this(name, description, initLower(name), icon, tool);
  }

  protected SalLanguageFileType(String name, Icon icon, BaseTool tool) {
    this(name, name, icon, tool);
  }
}
