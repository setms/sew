package org.setms.sew.intellij.filetype;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import javax.swing.Icon;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.km.domain.model.tool.Tool;
import org.setms.sew.intellij.lang.LanguageFile;

public abstract class BaseLanguageFileType extends LanguageFileType {

  private final String name;
  private final String description;
  private final String extension;
  private final Icon icon;
  private final Tool tool;

  public BaseLanguageFileType(
      @NotNull Language language,
      String name,
      String description,
      String extension,
      Icon icon,
      Tool tool) {
    super(language);
    this.name = name;
    this.description = description;
    this.extension = extension;
    this.icon = icon;
    this.tool = tool;
  }

  @Override
  public @NonNls @NotNull String getName() {
    return name;
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return getName();
  }

  @Override
  public @NlsContexts.Label @NotNull String getDescription() {
    return description;
  }

  @Override
  public @NlsSafe @NotNull String getDefaultExtension() {
    return extension;
  }

  @Override
  public Icon getIcon() {
    return icon;
  }

  public Tool getTool() {
    return tool;
  }

  public PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    return new LanguageFile(viewProvider, this);
  }
}
