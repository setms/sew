package org.setms.swe.domain.model.sdlc.technology;

import java.util.List;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;

/** Generates production code from domain artifacts. */
public interface CodeGenerator {

  List<CodeArtifact> generate(Command command);
}
