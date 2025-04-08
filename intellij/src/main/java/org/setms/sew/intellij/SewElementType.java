package org.setms.sew.intellij;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class SewElementType extends IElementType {
  public SewElementType(@NotNull @NonNls String debugName) {
    super(debugName, SewLanguage.INSTANCE);
  }
}
