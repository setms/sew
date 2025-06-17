package org.setms.sew.intellij.lang.acceptance;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AcceptanceTokenType extends IElementType {

  public AcceptanceTokenType(@NotNull @NonNls String debugName) {
    // same language instance you used in SewElementType
    super(debugName, AcceptanceLanguage.INSTANCE);
  }
}
