package org.setms.sew.intellij.filetype;

public class DecisionFileType extends SewLanguageFileType {

  public static final DecisionFileType INSTANCE = new DecisionFileType();

  private DecisionFileType() {
    super("Decision", "Architecture or design decision", SewIcons.DECISION, null);
  }
}
