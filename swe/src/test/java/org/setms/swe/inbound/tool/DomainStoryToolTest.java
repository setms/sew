package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.domainstory.DomainStory;

public class DomainStoryToolTest extends ToolTestCase<DomainStory> {

  protected DomainStoryToolTest() {
    super(new DomainStoryTool(), DomainStory.class, "main/requirements/domain-stories");
  }

  @Override
  protected void assertBuild(Resource<?> resource) {
    var output = resource.select("build/NonuserCantDeleteData");
    Stream.of("html", "png")
        .map("NonuserCantDeleteData.%s"::formatted)
        .map(output::select)
        .map(this::toFile)
        .forEach(file -> assertThat(file).isFile());
  }
}
