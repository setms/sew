package org.setms.swe.inbound.format.xml;

import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.Parser;

/** XML-based format that supports deeply nested structures. */
public class XmlFormat implements Format {

  public static final XmlFormat INSTANCE = new XmlFormat();

  @Override
  public Parser newParser() {
    return new XmlFormatParser();
  }

  @Override
  public Builder newBuilder() {
    return new XmlFormatBuilder();
  }
}
