package org.setms.swe.domain.model.sdlc.code;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.DataString;
import org.setms.km.domain.model.format.RootObject;

class CodeBuilder implements Builder {

  @Override
  public void build(RootObject root, PrintWriter writer) throws IOException {
    Optional.ofNullable(root.property("code", DataString.class))
        .map(DataString::getValue)
        .ifPresent(writer::print);
  }
}
