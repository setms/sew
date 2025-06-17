package org.setms.sew.intellij.lang.sew;

import com.intellij.lang.Language;

public class SewLanguage extends Language {

  public static final SewLanguage INSTANCE = new SewLanguage();
  public static final String ID = "Sew";

  private SewLanguage() {
    super(ID);
  }
}
