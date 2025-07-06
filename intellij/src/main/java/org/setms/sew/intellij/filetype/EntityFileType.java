package org.setms.sew.intellij.filetype;

import org.setms.sew.core.inbound.tool.EntityTool;

public class EntityFileType extends SewLanguageFileType {

  public static final EntityFileType INSTANCE = new EntityFileType();

  private EntityFileType() {
    super("Entity", "Structured data", SewIcons.ENTITY, new EntityTool());
  }
}
