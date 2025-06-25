package org.setms.sew.core.inboud.format.acceptance;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.setms.sew.core.domain.model.format.DataEnum;
import org.setms.sew.core.domain.model.format.DataList;
import org.setms.sew.core.domain.model.format.Format;
import org.setms.sew.core.domain.model.format.NestedObject;
import org.setms.sew.core.domain.model.format.Reference;
import org.setms.sew.core.domain.model.format.RootObject;
import org.setms.sew.core.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.sew.core.inbound.format.acceptance.AcceptanceFormat;

class AcceptanceFormatTest {

  private static final String SIMPLE_SCENARIO =
"""
| type      | name     |
| --------- | -------- |
| aggregate | ape.Bear |

| variable | type             | definition |
| -------- | ---------------- | ---------- |
| how      | Text             | Nonempty   |
| command  | command(DoIt)    |            |
| event    | event(ItWasDone) | How=how    |

| scenario     | init | command | state | emitted |
| ------------ | ---- | ------- | ----- | ------- |
| "Happy path" |      | command |       | event   |
""";
  private static final RootObject SIMPLE_SCENARIO_OBJECT =
      new RootObject("ape", "acceptanceTest", "aggregate.Bear")
          .set("sut", new Reference("aggregate", "Bear"))
          .set(
              "variables",
              new DataList()
                  .add(
                      new NestedObject("how")
                          .set("type", new DataEnum("Text"))
                          .set("definition", new DataEnum("Nonempty")))
                  .add(new NestedObject("command").set("type", new Reference("command", "DoIt")))
                  .add(
                      new NestedObject("event")
                          .set("type", new Reference("event", "ItWasDone"))
                          .set(
                              "definition",
                              new NestedObject("definition")
                                  .set("How", new Reference("variable", "how")))))
          .set(
              "scenarios",
              new DataList()
                  .add(
                      new NestedObject("Happy path")
                          .set("command", new Reference("variable", "command"))
                          .set("emitted", new Reference("variable", "event"))));
  private final Format format = new AcceptanceFormat();
  private final ByteArrayOutputStream output = new ByteArrayOutputStream();

  @Test
  void shouldParseSimpleScenario() throws IOException {
    assertDeserialization(SIMPLE_SCENARIO, SIMPLE_SCENARIO_OBJECT);
  }

  private void assertDeserialization(String data, RootObject expected) throws IOException {
    output.write(data.getBytes(UTF_8));

    var actual = format.newParser().parse(new ByteArrayInputStream(output.toByteArray()));

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void shouldBuildSimpleScenario() throws IOException {
    assertSerialization(SIMPLE_SCENARIO_OBJECT, SIMPLE_SCENARIO);
  }

  private void assertSerialization(RootObject data, String expected) throws IOException {
    format.newBuilder().build(data, output);

    assertThat(output).hasToString(expected);
  }

  @Test
  void shouldParseDomainObject() throws IOException {
    output.write(SIMPLE_SCENARIO.getBytes(UTF_8));

    var actual =
        format
            .newParser()
            .parse(new ByteArrayInputStream(output.toByteArray()), AcceptanceTest.class, true);

    assertThat(actual).isNotNull();
  }
}
