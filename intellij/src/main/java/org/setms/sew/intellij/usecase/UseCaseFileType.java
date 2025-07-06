package org.setms.sew.intellij.usecase;

import org.setms.sew.core.inbound.tool.UseCaseTool;
import org.setms.sew.intellij.filetype.SewIcons;
import org.setms.sew.intellij.filetype.SewLanguageFileType;

public class UseCaseFileType extends SewLanguageFileType {

  public static final UseCaseFileType INSTANCE = new UseCaseFileType();

  private UseCaseFileType() {
    super(
        "Use case",
        "Describes scenarios for user requirement",
        "useCase",
        SewIcons.USE_CASE,
        new UseCaseTool());
  }
}
