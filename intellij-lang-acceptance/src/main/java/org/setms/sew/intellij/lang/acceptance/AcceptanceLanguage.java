package org.setms.sew.intellij.lang.acceptance;

import com.intellij.lang.Language;

public class AcceptanceLanguage extends Language {

  public static final AcceptanceLanguage INSTANCE = new AcceptanceLanguage();
  public static final String ID = "Acceptance";

  private AcceptanceLanguage() {
    super(ID);
  }
}
