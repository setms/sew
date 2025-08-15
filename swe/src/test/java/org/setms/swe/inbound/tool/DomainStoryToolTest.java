package org.setms.swe.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;
import org.setms.swe.domain.model.sdlc.domainstory.DomainStory;

public class DomainStoryToolTest extends ToolTestCase<DomainStory> {

  protected DomainStoryToolTest() {
    super(new DomainStoryTool(), DomainStory.class, "main/requirements");
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
