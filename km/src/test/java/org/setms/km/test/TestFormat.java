package org.setms.km.test;

import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.Parser;

public class TestFormat implements Format {

  public static final TestFormat INSTANCE = new TestFormat();

  @Override
  public Parser newParser() {
    return new TestParser();
  }

  @Override
  public Builder newBuilder() {
    return new TestBuilder();
  }
}
