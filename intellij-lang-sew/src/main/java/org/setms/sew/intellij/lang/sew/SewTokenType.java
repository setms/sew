package org.setms.sew.intellij.lang.sew;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class SewTokenType extends IElementType {

  public SewTokenType(@NotNull @NonNls String debugName) {
    // same language instance you used in SewElementType
    super(debugName, SewLanguage.INSTANCE);
  }
}
