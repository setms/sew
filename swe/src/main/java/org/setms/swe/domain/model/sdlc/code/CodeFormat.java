package org.setms.swe.domain.model.sdlc.code;

import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.Parser;

@RequiredArgsConstructor
public class CodeFormat implements Format {

  private final ProgrammingLanguageConventions conventions;

  public CodeFormat() {
    this(null);
  }

  @Override
  public Parser newParser() {
    return new CodeParser(conventions);
  }

  @Override
  public Builder newBuilder() {
    return new CodeBuilder();
  }
}
