package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;

class EventToolTest extends ToolTestCase<Event> {

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
}
