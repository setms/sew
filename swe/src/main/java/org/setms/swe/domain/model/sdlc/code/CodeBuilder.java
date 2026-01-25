package org.setms.swe.domain.model.sdlc.code;

import java.io.IOException;
import java.io.PrintWriter;
import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.DataString;
import org.setms.km.domain.model.format.RootObject;

class CodeBuilder implements Builder {

  @Override
  public void build(RootObject root, PrintWriter writer) throws IOException {
    writer.print(root.property("code", DataString.class).getValue());
  }
}
