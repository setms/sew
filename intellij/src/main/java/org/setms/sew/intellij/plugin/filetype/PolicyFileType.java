package org.setms.sew.intellij.plugin.filetype;

public class PolicyFileType extends SalLanguageFileType {

  public static final PolicyFileType INSTANCE = new PolicyFileType();

  private PolicyFileType() {
    super("Policy", "Policy that handles events and issues commands", SewIcons.POLICY, null);
  }
}
