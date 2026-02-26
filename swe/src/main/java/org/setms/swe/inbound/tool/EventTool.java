package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

public class EventTool extends DtoCodeTool<Event> {

  public EventTool() {}

  EventTool(TechnologyResolver resolver) {
    super(resolver);
  }

  @Override
  public Set<Input<? extends Event>> validationTargets() {
    return Set.of(Inputs.events());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return Stream.of(Inputs.entities(), Inputs.decisions(), Inputs.initiatives())
        .collect(Collectors.toSet());
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> eventResource,
      Event event,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs) {
    return switch (suggestionCode) {
      case CREATE_PAYLOAD -> createPayloadFor(eventResource, event);
      default -> unknown(suggestionCode);
    };
  }
}
