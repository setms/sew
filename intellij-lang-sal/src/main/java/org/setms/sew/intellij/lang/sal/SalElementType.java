package org.setms.sew.intellij.lang.sal;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class SalElementType extends IElementType {

  public SalElementType(@NotNull @NonNls String debugName) {
    super(debugName, SalLanguage.INSTANCE);
  }

  @Override
  public String toString() {
    return "%s.%s".formatted(getClass().getSimpleName(), super.toString());
  }
}
