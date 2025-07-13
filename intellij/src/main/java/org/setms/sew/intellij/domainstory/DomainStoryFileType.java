package org.setms.sew.intellij.domainstory;

import org.setms.sew.core.inbound.tool.DomainStoryTool;
import org.setms.sew.intellij.filetype.SalLanguageFileType;
import org.setms.sew.intellij.filetype.SewIcons;

public class DomainStoryFileType extends SalLanguageFileType {

  public static final DomainStoryFileType INSTANCE = new DomainStoryFileType();

  private DomainStoryFileType() {
    super("DomainStory", SewIcons.DOMAIN_STORY, new DomainStoryTool());
  }
}
