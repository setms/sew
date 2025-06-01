package org.setms.sew.intellij.language.sew;

import static java.util.stream.Collectors.joining;
import static org.setms.sew.intellij.SewElementTypes.*;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.tool.UseCaseTool;
import org.setms.sew.intellij.SewElementTypes;
import org.setms.sew.intellij.editor.VirtualFileInputSource;
import org.setms.sew.intellij.filetype.UseCaseFileType;
import org.setms.sew.intellij.language.LevelSeverity;

public class SewAnnotator implements Annotator {

  private static final Collection<IElementType> PUNCTUATION =
      Set.of(COMMA, COMMENT, DOT, EQ, LBRACE, LBRACK, LPAREN, NEWLINE, RBRACE, RBRACK, RPAREN);
  private static final Map<String, Tool> TOOLS_BY_EXTENSION =
      Map.of(UseCaseFileType.INSTANCE.getDefaultExtension(), new UseCaseTool());

  @Override
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {
    if (psiElement instanceof PsiWhiteSpace
        || PUNCTUATION.contains(psiElement.getNode().getElementType())) {
      return;
    }
    var file = psiElement.getContainingFile();
    if (file == null || !file.isValid()) {
      return;
    }

    var documentManager = PsiDocumentManager.getInstance(file.getProject());
    var document = documentManager.getDocument(file);
    if (document == null || documentManager.isUncommited(document)) {
      return;
    }

    var location = locationOf(psiElement);
    if (location == null) {
      return;
    }

    var maybeTool =
        Optional.ofNullable(TOOLS_BY_EXTENSION.get(file.getVirtualFile().getExtension()));
    if (maybeTool.isEmpty()) {
      return;
    }
    var tool = maybeTool.get();
    var diagnostics = validateFile(tool, file);
    if (diagnostics.isEmpty()) {
      return;
    }

    diagnostics.stream()
        .filter(
            diagnostic ->
                diagnostic.location() == null
                    ? location.isEmpty()
                    : diagnostic.location().toString().equals(location))
        .forEach(
            diagnostic -> {
              var builder =
                  holder
                      .newAnnotation(LevelSeverity.of(diagnostic.level()), diagnostic.message())
                      .range(psiElement);
              for (var suggestion : diagnostic.suggestions()) {
                builder
                    .newFix(new ApplySuggestion(tool, suggestion, location, psiElement))
                    .registerFix();
              }
              builder.create();
            });
  }

  private Set<Diagnostic> validateFile(Tool tool, PsiFile file) {
    return tool.validate(new VirtualFileInputSource(file, tool));
  }

  private String locationOf(PsiElement psiElement) {
    if (psiElement instanceof LeafPsiElement leaf
        && leaf.getParent().getNode().getElementType() == SewElementTypes.QUALIFIED_NAME) {
      return "";
    }
    if (psiElement.getNode().getElementType() == SewElementTypes.OBJECT_START) {
      var rootObject = rootObjectOf(psiElement);
      return toLocation(rootObject, psiElement);
    }
    if (psiElement.getNode().getElementType() == SewElementTypes.LIST_ITEM) {
      var propertyName = propertyNameOf(psiElement);
      var containingObject = containingObjectOf(psiElement);
      var rootObject = rootObjectOf(psiElement);
      return "%s[%d]"
          .formatted(
              toLocation(rootObject, containingObject, propertyName), itemIndexOf(psiElement));
    }
    return null;
  }

  private PsiElement rootObjectOf(PsiElement psiElement) {
    return psiElement.getContainingFile().getChildren()[1].getChildren()[0];
  }

  private String toLocation(PsiElement... elements) {
    return Arrays.stream(elements)
        .distinct()
        .map(element -> element.getText().replace(' ', '/'))
        .collect(joining("/"));
  }

  private PsiElement propertyNameOf(PsiElement psiElement) {
    return ancestorOfType(psiElement, SewElementTypes.PROPERTY).getFirstChild();
  }

  private PsiElement ancestorOfType(PsiElement psiElement, IElementType type) {
    var result = psiElement;
    while (result.getNode().getElementType() != type) {
      result = result.getParent();
    }
    return result;
  }

  private PsiElement containingObjectOf(PsiElement psiElement) {
    return ancestorOfType(psiElement, SewElementTypes.OBJECT).getFirstChild();
  }

  private int itemIndexOf(PsiElement listItem) {
    var property = ancestorOfType(listItem, SewElementTypes.PROPERTY);
    var values = property.getChildren()[1].getFirstChild().getChildren();
    return Arrays.asList(values).indexOf(listItem);
  }
}
