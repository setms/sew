package org.setms.swe.inbound.tool;

import static org.setms.swe.inbound.tool.Inputs.decisions;

import java.util.Collection;
import java.util.Set;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.StandaloneTool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.TopicProvider;

public class ArchitectureTool extends StandaloneTool {

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    return Set.of(decisions());
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    /*
    var decisions = inputs.get(Decision.class);
    Topics.providers()
        .filter(provider -> hasAllUpstreamDecisions(provider, decisions))
        .map(TopicProvider::topics)
        .flatMap(Collection::stream)
        .filter(topic -> !hasDecisionFor(topic, decisions))
        .forEach(
            topic ->
                diagnostics.add(
                    new Diagnostic(
                        WARN,
                        "Missing decision for %s".formatted(initLower(toFriendlyName(topic))))));
     */
  }

  private boolean hasAllUpstreamDecisions(TopicProvider provider, Collection<Decision> decisions) {
    return provider.dependsOn().stream().allMatch(topic -> hasDecisionFor(topic, decisions));
  }

  private boolean hasDecisionFor(String topic, Collection<Decision> decisions) {
    return decisions.stream()
        .anyMatch(
            decision ->
                topic.equals(decision.getTopic())
                    && decision.getChoice() != null
                    && !decision.getChoice().isBlank());
  }
}
