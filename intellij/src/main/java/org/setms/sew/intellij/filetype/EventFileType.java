package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.lang.sew.SewLanguage;

public class EventFileType extends LanguageFileType {

  public static final EventFileType INSTANCE = new EventFileType();

  private EventFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "Event";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Event";
  }

  @Override
  public @NotNull String getDescription() {
    return "Something that happens that's interesting from a business perspective";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "event";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.EVENT;
  }
}
