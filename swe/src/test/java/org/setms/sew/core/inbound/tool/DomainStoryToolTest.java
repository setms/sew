package org.setms.sew.core.inbound.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.setms.km.outbound.tool.file.FileOutputSink;
import org.setms.sew.core.domain.model.sdlc.domainstory.DomainStory;

public class DomainStoryToolTest extends ToolTestCase<DomainStory> {

  protected DomainStoryToolTest() {
    super(new DomainStoryTool(), DomainStory.class, "main/requirements");
  }

  @Override
  protected void assertBuild(FileOutputSink sink) {
    var output = sink.select("reports/domainStories");
    Stream.of("html", "png")
        .map("NonuserCantDeleteData.%s"::formatted)
        .map(output::select)
        .map(FileOutputSink::getFile)
        .forEach(file -> assertThat(file).isFile());
  }
}
