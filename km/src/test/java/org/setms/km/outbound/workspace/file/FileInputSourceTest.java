package org.setms.km.outbound.workspace.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.workspace.Glob;

class FileInputSourceTest {

  private final FileInputSource inputSource = new FileInputSource(new File("."));

  @Test
  void shouldMatchPattern() {
    var actual = inputSource.matching(new Glob("src/test/java", "**/*Test.java"));

    assertThat(actual)
        .map(FileInputSource::getFile)
        .map(File::getName)
        .contains(getClass().getSimpleName() + ".java");
  }
}
