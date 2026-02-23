package org.setms.swe.domain.model.sdlc.code.java;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.technology.CodeGenerator;

@RequiredArgsConstructor
public class JavaCodeGenerator implements CodeGenerator {

  private final String topLevelPackage;

  @Override
  public List<CodeArtifact> generate(Command command) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
}
