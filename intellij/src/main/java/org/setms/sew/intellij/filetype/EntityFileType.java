package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.lang.sew.SewLanguage;

public class EntityFileType extends LanguageFileType {

  public static final EntityFileType INSTANCE = new EntityFileType();

  private EntityFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Entity";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Entity";
  }

  @Override
  public @NotNull String getDescription() {
    return "Structured data";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "entity";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.ENTITY;
  }
}
