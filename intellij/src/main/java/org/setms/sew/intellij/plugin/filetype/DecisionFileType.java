package org.setms.sew.intellij.plugin.filetype;

public class DecisionFileType extends SalLanguageFileType {

  public static final DecisionFileType INSTANCE = new DecisionFileType();

  private DecisionFileType() {
    super("Decision", "Architecture or design decision", SewIcons.DECISION, null);
  }
}
