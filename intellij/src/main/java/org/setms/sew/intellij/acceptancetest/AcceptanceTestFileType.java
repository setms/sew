package org.setms.sew.intellij.acceptancetest;

import org.setms.sew.core.inbound.tool.AcceptanceTestTool;
import org.setms.sew.intellij.filetype.BaseLanguageFileType;
import org.setms.sew.intellij.filetype.SewIcons;
import org.setms.sew.intellij.lang.acceptance.AcceptanceLanguage;
import org.setms.sew.intellij.lang.acceptance.AcceptanceParserDefinition;

public class AcceptanceTestFileType extends BaseLanguageFileType {

  public static final AcceptanceTestFileType INSTANCE = new AcceptanceTestFileType();

  private AcceptanceTestFileType() {
    super(
        AcceptanceLanguage.INSTANCE,
        "AcceptanceTest",
        "Acceptance test",
        "acceptance",
        SewIcons.ACCEPTANCE,
        new AcceptanceTestTool());
    AcceptanceParserDefinition.addFileType(this);
  }
}
