package org.setms.sew.intellij.plugin.domain;

import org.setms.swe.inbound.tool.DomainTool;
import org.setms.sew.intellij.plugin.filetype.SalLanguageFileType;
import org.setms.sew.intellij.plugin.filetype.SewIcons;

public class DomainFileType extends SalLanguageFileType {

  public static final DomainFileType INSTANCE = new DomainFileType();

  private DomainFileType() {
    super("Domain", SewIcons.DOMAIN, new DomainTool());
  }
}
