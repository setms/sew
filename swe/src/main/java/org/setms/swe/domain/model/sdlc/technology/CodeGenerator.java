package org.setms.swe.domain.model.sdlc.technology;

import java.util.List;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;

/** Generates production code from domain artifacts. */
public interface CodeGenerator {

  List<CodeArtifact> generate(Command command, Entity payload);

  List<CodeArtifact> generate(Event event, Entity payload);
}
