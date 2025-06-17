package org.setms.sew.intellij.lang.acceptance;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AcceptanceElementType extends IElementType {

  public AcceptanceElementType(@NotNull @NonNls String debugName) {
    super(debugName, AcceptanceLanguage.INSTANCE);
  }

  @Override
  public String toString() {
    return "SewElementType." + super.toString();
  }
}
