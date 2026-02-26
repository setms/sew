package org.setms.swe.inbound.tool;

import java.util.Collection;
import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

public class EventTool extends ArtifactTool<Event> {

  private final TechnologyResolver resolver;

  public EventTool() {
    this(new TechnologyResolverImpl());
  }

  EventTool(TechnologyResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public Set<Input<? extends Event>> validationTargets() {
    return Set.of(Inputs.events());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return Set.of();
  }

  @Override
  public void validate(Event event, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (event.getPayload() == null || resolver.codeGenerator(inputs, diagnostics).isEmpty()) {
      return;
    }
  }
}
