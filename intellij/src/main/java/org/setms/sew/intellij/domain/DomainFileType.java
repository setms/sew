package org.setms.sew.intellij.domain;

import org.setms.sew.core.inbound.tool.DomainTool;
import org.setms.sew.intellij.filetype.SalLanguageFileType;
import org.setms.sew.intellij.filetype.SewIcons;

public class DomainFileType extends SalLanguageFileType {

  public static final DomainFileType INSTANCE = new DomainFileType();

  private DomainFileType() {
    super("Domain", SewIcons.DOMAIN, new DomainTool());
  }
}
