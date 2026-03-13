package org.setms.swe.domain.model.sdlc.technology;

import java.util.List;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;

public interface FrameworkCodeGenerator {

  List<CodeArtifact> generateEntityFor(DatabaseSchema schema, Resource<?> resource);

  List<CodeArtifact> generateEndpointFor(
      Resource<?> resource,
      Aggregate aggregate,
      Command command,
      Entity commandPayload,
      Event event);
}
