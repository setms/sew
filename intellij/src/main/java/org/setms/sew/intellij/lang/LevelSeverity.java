package org.setms.sew.intellij.lang;

import com.intellij.lang.annotation.HighlightSeverity;
import org.setms.km.domain.model.validation.Level;

public class LevelSeverity {

  public static HighlightSeverity of(Level level) {
    return switch (level) {
      case ERROR -> HighlightSeverity.ERROR;
      case WARN -> HighlightSeverity.WARNING;
      case INFO -> HighlightSeverity.INFORMATION;
    };
  }
}
