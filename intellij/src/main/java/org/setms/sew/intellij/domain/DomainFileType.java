package org.setms.sew.intellij.domain;

import org.setms.sew.core.inbound.tool.DomainTool;
import org.setms.sew.intellij.filetype.SewIcons;
import org.setms.sew.intellij.filetype.SewLanguageFileType;

public class DomainFileType extends SewLanguageFileType {

  public static final DomainFileType INSTANCE = new DomainFileType();

  private DomainFileType() {
    super("Domain", SewIcons.DOMAIN, new DomainTool());
  }
}
