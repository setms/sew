package org.setms.sew.core.inbound.format.acceptance;

import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.Parser;

public class AcceptanceFormat implements Format {

  @Override
  public Parser newParser() {
    return new AcceptanceFormatParser();
  }

  @Override
  public Builder newBuilder() {
    return new AcceptanceFormatBuilder();
  }
}
