package org.setms.sew.intellij.plugin.acceptancetest;

import org.setms.sew.intellij.lang.acceptance.AcceptanceLanguage;
import org.setms.sew.intellij.lang.acceptance.AcceptanceParserDefinition;
import org.setms.sew.intellij.plugin.filetype.BaseLanguageFileType;
import org.setms.sew.intellij.plugin.filetype.SewIcons;
import org.setms.swe.inbound.tool.AcceptanceTestTool;

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
