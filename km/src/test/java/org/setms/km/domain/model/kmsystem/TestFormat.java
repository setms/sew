package org.setms.km.domain.model.kmsystem;

import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.Parser;

class TestFormat implements Format {

  @Override
  public Parser newParser() {
    return new TestParser();
  }

  @Override
  public Builder newBuilder() {
    return new TestBuilder();
  }
}
