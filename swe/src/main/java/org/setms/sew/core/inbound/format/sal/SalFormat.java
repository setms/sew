package org.setms.sew.core.inbound.format.sal;

import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.Parser;

public class SalFormat implements Format {

  @Override
  public Parser newParser() {
    return new SalFormatParser();
  }

  @Override
  public Builder newBuilder() {
    return new SalFormatBuilder();
  }
}
