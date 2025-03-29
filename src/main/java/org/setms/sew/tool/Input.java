package org.setms.sew.tool;

import lombok.Value;
import org.setms.sew.format.Format;

@Value
@SuppressWarnings("ClassCanBeRecord")
public class Input {

  Glob glob;
  Format format;
}
