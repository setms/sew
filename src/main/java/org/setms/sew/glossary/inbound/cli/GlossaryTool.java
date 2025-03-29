package org.setms.sew.glossary.inbound.cli;

import java.util.Collection;
import java.util.List;
import org.setms.sew.format.sew.SewFormat;
import org.setms.sew.tool.Glob;
import org.setms.sew.tool.Input;
import org.setms.sew.tool.Tool;

public class GlossaryTool implements Tool {

  @Override
  public Collection<Input> getInputs() {
    return List.of(new Input(new Glob("src/main/glossary", "**/*.term"), new SewFormat()));
  }
}
