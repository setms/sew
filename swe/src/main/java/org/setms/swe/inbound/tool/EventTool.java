package org.setms.swe.inbound.tool;

import static java.util.stream.Collectors.toSet;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.swe.inbound.tool.Inputs.code;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.entities;
import static org.setms.swe.inbound.tool.Inputs.initiatives;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.design.Entity;
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
    return Stream.of(Stream.of(entities(), decisions(), initiatives()), code().stream())
        .flatMap(s -> s)
        .collect(toSet());
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
      case GENERATE_CODE -> generateCodeFor(eventResource, event, inputs);
      default -> unknown(suggestionCode);
    };
  }

  private AppliedSuggestion generateCodeFor(
      Resource<?> eventResource, Event event, ResolvedInputs inputs) {
    var diagnostics = new ArrayList<Diagnostic>();
    return resolver
        .codeGenerator(inputs, diagnostics)
        .flatMap(
            generator ->
                Optional.ofNullable(event.getPayload())
                    .flatMap(link -> link.resolveFrom(inputs.get(Entity.class)))
                    .map(entity -> writeCode(generator.generate(event, entity), eventResource)))
        .orElseGet(AppliedSuggestion::none);
  }
}
