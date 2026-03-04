package org.setms.swe.domain.model.sdlc.technology;

import java.util.List;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;

/** Generates framework-specific code from domain artifacts. */
public interface FrameworkCodeGenerator {

  List<CodeArtifact> generateControllerFor(
      Aggregate aggregate, Command command, Entity commandPayload, Event event);
}
