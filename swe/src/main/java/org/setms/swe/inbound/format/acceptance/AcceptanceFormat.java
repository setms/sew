package org.setms.swe.inbound.format.acceptance;

import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.Parser;

public class AcceptanceFormat implements Format {

  public static final AcceptanceFormat INSTANCE = new AcceptanceFormat();

  @Override
  public Parser newParser() {
    return new AcceptanceFormatParser();
  }

  @Override
  public Builder newBuilder() {
    return new AcceptanceFormatBuilder();
  }
}
