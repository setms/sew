package org.setms.sew.core.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.sew.core.domain.model.sdlc.domainstory.DomainStory;

public class DomainStoryToolTest extends ToolTestCase<DomainStory> {

  protected DomainStoryToolTest() {
    super(new DomainStoryTool(), DomainStory.class, "main/requirements");
  }

  @Override
  protected void assertBuild(Resource<?> resource) {
    var output = resource.select("build/reports/domainStories");
    Stream.of("html", "png")
        .map("NonuserCantDeleteData.%s"::formatted)
        .map(output::select)
        .map(this::toFile)
        .forEach(file -> assertThat(file).isFile());
  }
}
