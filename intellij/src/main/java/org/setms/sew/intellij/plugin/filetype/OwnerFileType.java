package org.setms.sew.intellij.plugin.filetype;


public class OwnerFileType extends SalLanguageFileType {

  public static final OwnerFileType INSTANCE = new OwnerFileType();

  private OwnerFileType() {
    super("Owner", "Stakeholder who finances the development of the system", SewIcons.OWNER);
  }
}
