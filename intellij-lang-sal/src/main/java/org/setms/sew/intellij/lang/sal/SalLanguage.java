package org.setms.sew.intellij.lang.sal;

import com.intellij.lang.Language;

public class SalLanguage extends Language {

  public static final SalLanguage INSTANCE = new SalLanguage();
  public static final String ID = "Sal";

  private SalLanguage() {
    super(ID);
  }
}
