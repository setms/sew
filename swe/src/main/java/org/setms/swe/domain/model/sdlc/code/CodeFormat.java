package org.setms.swe.domain.model.sdlc.code;

import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.Parser;

public class CodeFormat implements Format {

  public static final CodeFormat INSTANCE = new CodeFormat();

  @Override
  public Parser newParser() {
    return new CodeParser();
  }

  @Override
  public Builder newBuilder() {
    return new CodeBuilder();
  }
}
