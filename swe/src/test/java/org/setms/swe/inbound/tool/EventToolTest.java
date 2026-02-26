package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;

class EventToolTest extends ResolverToolTestCase<Event> {

  EventToolTest() {
    super(new EventTool(), Event.class, "main/design");
  }

  @Test
  void shouldNotWarnAboutMissingCodeWhenEventHasNoPayload() {
    var event = new Event(new FullyQualifiedName("design", "WithoutPayload"));
    var diagnostics = new ArrayList<Diagnostic>();

    ((EventTool) getTool()).validate(event, new ResolvedInputs(), diagnostics);

    assertThat(diagnostics).isEmpty();
  }

  @Test
  void shouldReportDiagnosticsFromResolverWhenCodeIsMissing() {
    var resolver = mock(TechnologyResolver.class);
    var tool = new EventTool(resolver);
    var diagnostic = givenResolverAddingDiagnostic(resolver);
    var diagnostics = new ArrayList<Diagnostic>();

    tool.validate(givenEventWithPayload(), givenResolvedPayload(), diagnostics);

    assertThat(diagnostics).containsExactly(diagnostic);
  }

  private Event givenEventWithPayload() {
    return new Event(new FullyQualifiedName("design", "TodoItemAdded"))
        .setPayload(new Link("entity", "Payload"));
  }

  private ResolvedInputs givenResolvedPayload() {
    return new ResolvedInputs()
        .put("entities", List.of(new Entity(new FullyQualifiedName("design", "Payload"))));
  }
}
