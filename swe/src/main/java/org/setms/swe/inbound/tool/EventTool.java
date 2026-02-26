package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

public class EventTool extends PayloadCodeTool<Event> {

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
  public void validate(Event event, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (event.getPayload() == null || resolver.codeGenerator(inputs, diagnostics).isEmpty()) {
      return;
    }
    if (codeFor(event, inputs).isEmpty()) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing event DTO",
              event.toLocation(),
              new Suggestion(GENERATE_CODE, "Generate event DTO")));
    }
  }
}
