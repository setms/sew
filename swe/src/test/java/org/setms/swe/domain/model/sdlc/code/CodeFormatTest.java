package org.setms.swe.domain.model.sdlc.code;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.format.DataString;
import org.setms.km.domain.model.format.RootObject;

class CodeFormatTest {

  private static final String CODE =
      """
      language: MadeUp;
      package:  root;
      code:     begin print("Hello, world!"); end
      """;

  private final CodeFormat codeFormat = new CodeFormat();

  @Test
  void shouldParseCode() throws IOException {
    var actual = codeFormat.newParser().parse(new ByteArrayInputStream(CODE.getBytes(UTF_8)));

    assertThat(actual.property("code", DataString.class).getValue()).isEqualTo(CODE);
  }

  @Test
  void shouldBuildCode() throws IOException {
    var code = new RootObject(null, null, null).set("code", new DataString(CODE));
    var output = new ByteArrayOutputStream();

    codeFormat.newBuilder().build(code, output);

    assertThat(output).hasToString(CODE);
  }
}
