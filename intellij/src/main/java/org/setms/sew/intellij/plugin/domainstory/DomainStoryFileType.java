package org.setms.sew.intellij.plugin.domainstory;

import org.setms.sew.intellij.plugin.filetype.SalLanguageFileType;
import org.setms.sew.intellij.plugin.filetype.SewIcons;

public class DomainStoryFileType extends SalLanguageFileType {

  public static final DomainStoryFileType INSTANCE = new DomainStoryFileType();

  private DomainStoryFileType() {
    super("DomainStory", SewIcons.DOMAIN_STORY);
  }
}
