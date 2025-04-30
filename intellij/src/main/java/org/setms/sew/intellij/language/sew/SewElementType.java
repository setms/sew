package org.setms.sew.intellij.language.sew;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class SewElementType extends IElementType {

  public SewElementType(@NotNull @NonNls String debugName) {
    super(debugName, SewLanguage.INSTANCE);
  }

  @Override
  public String toString() {
    return "SewElementType." + super.toString();
  }
}
