package org.setms.km.domain.model.kmsystem;

import java.io.PrintWriter;
import org.setms.km.domain.model.format.Builder;
import org.setms.km.domain.model.format.RootObject;

class TestBuilder implements Builder {

  @Override
  public void build(RootObject root, PrintWriter writer) {
    writer.println(root.getScope());
    writer.println(root.getType());
    writer.println(root.getName());
  }
}
