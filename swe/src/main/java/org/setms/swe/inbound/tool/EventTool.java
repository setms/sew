package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.validation.Level.WARN;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

public class EventTool extends ArtifactTool<Event> {

  public static final String GENERATE_CODE = "code.generate";

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
    if (codeFor(event, inputs).isEmpty()) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing event DTO",
              event.toLocation(),
              new Suggestion(GENERATE_CODE, "Generate event DTO")));
    }
  }

  private Optional<CodeArtifact> codeFor(Event event, ResolvedInputs inputs) {
    return inputs.get(CodeArtifact.class).stream()
        .filter(
            ca -> ca.getName().equals(event.getName()) && ca.getPackage().endsWith(".domain.model"))
        .findFirst();
  }
}
