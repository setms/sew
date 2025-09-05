package org.setms.sew.intellij.plugin;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class CompletionContributor
    extends com.intellij.codeInsight.completion.CompletionContributor {

  private static final List<String> KEYWORDS =
      List.of(
          "activity",
          "aggregate",
          "alternative",
          "businessRequirement",
          "calendarEvent",
          "clockEvent",
          "command",
          "computerSystem",
          "decision",
          "domain",
          "domainStory",
          "entity",
          "event",
          "externalSystem",
          "field",
          "hotspot",
          "material",
          "module",
          "modules",
          "owner",
          "person",
          "people",
          "policy",
          "readModel",
          "scenario",
          "screen",
          "scope",
          "sentence",
          "subdomain",
          "term",
          "useCase",
          "user",
          "userRequirement",
          "valueObject",
          "workObject");

  public CompletionContributor() {
    extend(
        CompletionType.BASIC,
        PlatformPatterns.psiElement(PsiElement.class),
        new CompletionProvider<>() {
          @Override
          protected void addCompletions(
              @NotNull CompletionParameters parameters,
              @NotNull ProcessingContext context,
              @NotNull CompletionResultSet result) {
            for (var keyword : KEYWORDS) {
              result.addElement(
                  LookupElementBuilder.create(keyword)
                      .withBoldness(true)
                      .withTypeText("keyword", true));
            }
          }
        });
  }
}
