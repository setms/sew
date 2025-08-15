package org.setms.sew.intellij.plugin.filetype;

import static org.setms.km.domain.model.format.Strings.initLower;

import javax.swing.Icon;
import org.setms.sew.intellij.lang.sal.SalLanguage;
import org.setms.sew.intellij.lang.sal.SalParserDefinition;

public abstract class SalLanguageFileType extends BaseLanguageFileType {

  protected SalLanguageFileType(
      String name, String description, String extension, Icon icon) {
    super(SalLanguage.INSTANCE, name, description, extension, icon);
    SalParserDefinition.addFileType(this);
  }

  protected SalLanguageFileType(String name, String description, Icon icon) {
    this(name, description, initLower(name), icon);
  }

  protected SalLanguageFileType(String name, Icon icon) {
    this(name, name, icon);
  }
}
