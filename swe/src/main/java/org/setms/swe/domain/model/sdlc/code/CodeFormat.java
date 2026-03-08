package org.setms.swe.domain.model.sdlc.code;

import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.Parser;
import org.setms.swe.domain.model.sdlc.technology.NameExtractor;

@RequiredArgsConstructor
public class CodeFormat implements Format {

  private final NameExtractor nameExtractor;

  public CodeFormat() {
    this(null);
  }

  @Override
  public Parser newParser() {
    return new CodeParser(nameExtractor);
  }

  @Override
  public Builder newBuilder() {
    return new CodeBuilder();
  }
}
