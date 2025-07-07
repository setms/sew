package org.setms.sew.intellij.usecase;

import org.setms.sew.core.inbound.tool.UseCaseTool;
import org.setms.sew.intellij.filetype.SalLanguageFileType;
import org.setms.sew.intellij.filetype.SewIcons;

public class UseCaseFileType extends SalLanguageFileType {

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
