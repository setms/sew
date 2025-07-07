package org.setms.sew.intellij.lang.sal;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class SalTokenType extends IElementType {

  public SalTokenType(@NotNull @NonNls String debugName) {
    super(debugName, SalLanguage.INSTANCE);
  }
}
