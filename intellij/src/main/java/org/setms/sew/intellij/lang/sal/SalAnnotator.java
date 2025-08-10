package org.setms.sew.intellij.lang.sal;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.format.Strings.initUpper;
import static org.setms.sew.intellij.lang.sal.SalElementTypes.*;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.sew.intellij.lang.LevelSeverity;
import org.setms.sew.intellij.plugin.filetype.SalLanguageFileType;
import org.setms.sew.intellij.plugin.km.KmSystemService;

public class SalAnnotator implements Annotator {

  private static final Collection<IElementType> PUNCTUATION =
      Set.of(COMMA, COMMENT, DOT, EQ, LBRACE, LBRACK, LPAREN, NEWLINE, RBRACE, RBRACK, RPAREN);

  private static String previousDocumentText;
  private static Set<Diagnostic> diagnostics = emptySet();

  @Override
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {
    if (psiElement instanceof PsiWhiteSpace
        || PUNCTUATION.contains(psiElement.getNode().getElementType())) {
      return;
    }
    var file = psiElement.getContainingFile();
    if (file == null || !file.isValid() || file.getVirtualFile() == null) {
      return;
    }
    if (!(file.getFileType() instanceof SalLanguageFileType)) {
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

    if (!document.getText().equals(previousDocumentText)) {
      previousDocumentText = document.getText();
      var service = psiElement.getProject().getService(KmSystemService.class);
      var resource = service.getWorkspace().find(file.getVirtualFile());
      diagnostics = service.getKmSystem().diagnosticsFor(resource.path());
    }
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
                    .newFix(new ApplySuggestion(suggestion, diagnostic.location(), psiElement))
                    .registerFix();
              }
              builder.create();
            });
  }

  private String locationOf(PsiElement psiElement) {
    if (psiElement instanceof LeafPsiElement leaf
        && leaf.getParent().getNode().getElementType() == SalElementTypes.QUALIFIED_NAME) {
      return psiElement.getText();
    }
    if (psiElement.getNode().getElementType() == SalElementTypes.OBJECT_START) {
      var rootObject = rootObjectOf(psiElement);
      return toLocation(rootObject, psiElement);
    }
    if (psiElement.getNode().getElementType() == SalElementTypes.LIST_ITEM) {
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
    return Stream.concat(
            Stream.of(elements[0].getParent().getParent().getChildren()[0].getChildren()[0]),
            Arrays.stream(elements))
        .distinct()
        .map(this::toPath)
        .collect(joining("/"));
  }

  private String toPath(PsiElement element) {
    if (element.getNode().getElementType() == OBJECT_START && !element.getText().contains(" ")) {
      var index = indexOf(element.getParent(), element.getParent().getParent().getChildren()) - 1;
      return "%s/%s%d".formatted(element.getText(), initUpper(element.getText()), index);
    }
    return element.getText().replace(' ', '/');
  }

  private int indexOf(PsiElement item, PsiElement[] items) {
    return Arrays.asList(items).indexOf(item);
  }

  private PsiElement propertyNameOf(PsiElement psiElement) {
    return ancestorOfType(psiElement, SalElementTypes.PROPERTY).getFirstChild();
  }

  private PsiElement ancestorOfType(PsiElement psiElement, IElementType type) {
    var result = psiElement;
    while (result.getNode().getElementType() != type) {
      result = result.getParent();
    }
    return result;
  }

  private PsiElement containingObjectOf(PsiElement psiElement) {
    return ancestorOfType(psiElement, SalElementTypes.OBJECT).getFirstChild();
  }

  private int itemIndexOf(PsiElement listItem) {
    var property = ancestorOfType(listItem, SalElementTypes.PROPERTY);
    var values = property.getChildren()[1].getFirstChild().getChildren();
    return indexOf(listItem, values);
  }
}
