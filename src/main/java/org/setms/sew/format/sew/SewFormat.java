package org.setms.sew.format.sew;

import org.setms.sew.format.Builder;
import org.setms.sew.format.Format;
import org.setms.sew.format.Parser;

public class SewFormat implements Format {

  @Override
  public Parser newParser() {
    return new SewParser();
  }

  @Override
  public Builder newBuilder() {
    return new SewBuilder();
  }
}
