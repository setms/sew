package org.setms.sew.intellij.filetype;

import static org.setms.sew.core.domain.model.format.Strings.initLower;

import javax.swing.Icon;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.intellij.lang.sew.SewLanguage;
import org.setms.sew.intellij.lang.sew.SewParserDefinition;

public abstract class SewLanguageFileType extends BaseLanguageFileType {

  protected SewLanguageFileType(
      String name, String description, String extension, Icon icon, Tool tool) {
    super(SewLanguage.INSTANCE, name, description, extension, icon, tool);
    SewParserDefinition.addFileType(this);
  }

  protected SewLanguageFileType(String name, String description, Icon icon, Tool tool) {
    this(name, description, initLower(name), icon, tool);
  }

  protected SewLanguageFileType(String name, Icon icon, Tool tool) {
    this(name, name, icon, tool);
  }

}
