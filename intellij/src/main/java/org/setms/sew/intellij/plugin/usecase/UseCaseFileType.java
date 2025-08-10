package org.setms.sew.intellij.plugin.usecase;

import org.setms.swe.inbound.tool.UseCaseTool;
import org.setms.sew.intellij.plugin.filetype.SalLanguageFileType;
import org.setms.sew.intellij.plugin.filetype.SewIcons;

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
