package org.setms.sew.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

class GlobTest {

  @Test
  void shouldMatchPattern() {
    var actual = new Glob("src/test/java", "**/*Test.java").matchingIn(new File("."));

    assertThat(actual).map(File::getName).contains(getClass().getSimpleName() + ".java");
  }
}
