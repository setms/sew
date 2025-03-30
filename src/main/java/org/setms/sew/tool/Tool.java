package org.setms.sew.tool;

import java.io.File;
import java.util.Collection;

public interface Tool {

  Collection<Input> getInputs();

  void run(File dir);
}
