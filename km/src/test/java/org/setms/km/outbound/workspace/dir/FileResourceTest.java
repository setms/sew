package org.setms.km.outbound.workspace.dir;

import java.io.File;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.workspace.Glob;

class FileResourceTest {

  private final FileResource resource = new FileResource(new File("."));

  @Test
  void shouldMatchPattern() {
    var actual = resource.matching(new Glob("src/test/java", "**/*Test.java"));

    Assertions.assertThat(actual)
        .map(FileResource::getFile)
        .map(File::getName)
        .contains(getClass().getSimpleName() + ".java");
  }
}
