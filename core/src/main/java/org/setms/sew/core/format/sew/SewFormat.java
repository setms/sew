package org.setms.sew.core.format.sew;

import org.setms.sew.core.format.Builder;
import org.setms.sew.core.format.Format;
import org.setms.sew.core.format.Parser;

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
