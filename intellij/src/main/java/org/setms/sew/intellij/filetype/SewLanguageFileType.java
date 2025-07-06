package org.setms.sew.intellij.filetype;

import static org.setms.sew.core.domain.model.format.Strings.initLower;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import javax.swing.Icon;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.intellij.lang.sew.SewLanguage;
import org.setms.sew.intellij.lang.sew.SewParserDefinition;

public abstract class SewLanguageFileType extends LanguageFileType {

  private final String name;
  private final String description;
  private final String extension;
  private final Icon icon;
  private final Tool tool;

  protected SewLanguageFileType(
      String name, String description, String extension, Icon icon, Tool tool) {
    super(SewLanguage.INSTANCE);
    this.name = name;
    this.description = description;
    this.extension = extension;
    this.icon = icon;
    this.tool = tool;
    SewParserDefinition.addFileType(this);
  }

  protected SewLanguageFileType(String name, String description, Icon icon, Tool tool) {
    this(name, description, initLower(name), icon, tool);
  }

  protected SewLanguageFileType(String name, Icon icon, Tool tool) {
    this(name, name, icon, tool);
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
    return new SewLanguageFile(viewProvider, this);
  }
}
