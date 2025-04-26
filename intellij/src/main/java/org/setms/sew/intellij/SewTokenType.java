package org.setms.sew.intellij;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class SewTokenType extends IElementType {

  public SewTokenType(@NotNull @NonNls String debugName) {
    super(debugName, SewLanguage.INSTANCE);
  }

  @Override
  public String toString() {
    return "SewTokenType." + super.toString();
  }
}
