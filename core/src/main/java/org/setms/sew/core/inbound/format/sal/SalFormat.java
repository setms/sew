package org.setms.sew.core.inbound.format.sal;

import org.setms.sew.core.domain.model.format.Builder;
import org.setms.sew.core.domain.model.format.Format;
import org.setms.sew.core.domain.model.format.Parser;

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
