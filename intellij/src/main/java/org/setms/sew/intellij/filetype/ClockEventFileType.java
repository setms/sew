package org.setms.sew.intellij.filetype;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.lang.sew.SewLanguage;

public class ClockEventFileType extends LanguageFileType {

  public static final ClockEventFileType INSTANCE = new ClockEventFileType();

  private ClockEventFileType() {
    super(SewLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getName() {
    return "ClockEvent";
  }

  @Override
  public @Nls @NotNull String getDisplayName() {
    return "Clock event";
  }

  @Override
  public @NotNull String getDescription() {
    return "Event that's triggered by the passing of time";
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return "clockEvent";
  }

  @Override
  public @Nullable Icon getIcon() {
    return SewIcons.CLOCK_EVENT;
  }
}
