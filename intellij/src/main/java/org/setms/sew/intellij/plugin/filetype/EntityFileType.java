package org.setms.sew.intellij.plugin.filetype;

import org.setms.swe.inbound.tool.EntityTool;

public class EntityFileType extends SalLanguageFileType {

  public static final EntityFileType INSTANCE = new EntityFileType();

  private EntityFileType() {
    super("Entity", "Structured data", SewIcons.ENTITY, new EntityTool());
  }
}
