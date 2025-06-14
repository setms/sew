package org.setms.sew.core.inbound.format.sew;

import org.setms.sew.core.domain.model.format.Builder;
import org.setms.sew.core.domain.model.format.Format;
import org.setms.sew.core.domain.model.format.Parser;

public class SewFormat implements Format {

  @Override
  public Parser newParser() {
    return new SewFormatParser();
  }

  @Override
  public Builder newBuilder() {
    return new SewFormatBuilder();
  }
}
