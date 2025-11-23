package org.setms.sew.intellij.plugin.filetype;

public class EntityFileType extends SalLanguageFileType {

  public static final EntityFileType INSTANCE = new EntityFileType();

  private EntityFileType() {
    super("Entity", "Structured data", SewIcons.ENTITY);
  }
}
