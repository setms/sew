package org.setms.sew.intellij.structure;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import java.util.ArrayList;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.SewElementTypes;

public class SewStructureViewElement extends PsiTreeElementBase<PsiElement> {

  public SewStructureViewElement(PsiElement psiElement) {
    super(psiElement);
  }

  @Override
  public @NotNull Collection<StructureViewTreeElement> getChildrenBase() {
    var result = new ArrayList<StructureViewTreeElement>();
    var current = getElement();
    if (current instanceof PsiFile file) {
      current = rootObjectIn(file);
      if (current == null) {
        return result;
      }
      for (var child : current.getParent().getChildren()) {
        if (child != current && isType(child, SewElementTypes.OBJECT)) {
          result.add(new SewStructureViewElement(child));
        }
      }
    }
    return result;
  }

  private PsiElement rootObjectIn(PsiFile file) {
    return childOfType(file, SewElementTypes.OBJECT);
  }

  private PsiElement childOfType(PsiElement element, IElementType type) {
    for (var child : element.getChildren()) {
      if (child.getNode() != null && isType(child, type)) {
        return child;
      }
    }
    return null;
  }

  private boolean isType(PsiElement element, IElementType type) {
    if (element == null) {
      return false;
    }
    return element.getNode().getElementType() == type;
  }

  @Override
  public @Nullable String getPresentableText() {
    if (getElement() instanceof PsiFile file) {
      return nameOf(rootObjectIn(file));
    }
    return nameOf(getElement());
  }

  private String nameOf(PsiElement object) {
    if (object == null) {
      return null;
    }
    var name = childOfType(object, SewElementTypes.OBJECT_NAME);
    return name == null ? null : name.getText();
  }
}
