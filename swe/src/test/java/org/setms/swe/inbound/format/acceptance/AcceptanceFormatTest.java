package org.setms.swe.inbound.format.acceptance;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.format.DataEnum;
import org.setms.km.domain.model.format.DataList;
import org.setms.km.domain.model.format.DataString;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.NestedObject;
import org.setms.km.domain.model.format.Reference;
import org.setms.km.domain.model.format.RootObject;

class AcceptanceFormatTest {

  private static final String AGGREGATE_SCENARIO =
      """
      | type      | name     |
      | --------- | -------- |
      | aggregate | ape.Bear |

      | variable | type             | definition |
      | -------- | ---------------- | ---------- |
      | how      | text             | nonempty   |
      | command  | command(DoIt)    |            |
      | event    | event(ItWasDone) | How=how    |

      | scenario     | init | accepts | state | emitted |
      | ------------ | ---- | ------- | ----- | ------- |
      | "Happy path" |      | command |       | event   |
      """;
  private static final RootObject AGGREGATE_SCENARIO_OBJECT =
      new RootObject("ape", "acceptanceTest", "BearAggregate")
          .set("sut", new Reference("aggregate", "Bear"))
          .set(
              "variables",
              new DataList()
                  .add(
                      new NestedObject("how")
                          .set("type", new DataEnum("text"))
                          .set("definitions", new DataList().add(new DataEnum("nonempty"))))
                  .add(new NestedObject("command").set("type", new Reference("command", "DoIt")))
                  .add(
                      new NestedObject("event")
                          .set("type", new Reference("event", "ItWasDone"))
                          .set(
                              "definitions",
                              new DataList()
                                  .add(
                                      new NestedObject("How")
                                          .set("fieldName", new DataString("How"))
                                          .set("value", new Reference("variable", "how"))))))
          .set(
              "scenarios",
              new DataList()
                  .add(
                      new NestedObject("Happy path")
                          .set("accepts", new Reference("variable", "command"))
                          .set("emitted", new Reference("variable", "event"))));
  private static final String POLICY_SCENARIO =
      """
      | type   | name     |
      | ------ | -------- |
      | policy | ape.Bear |

      | variable | type             | definition |
      | -------- | ---------------- | ---------- |
      | how      | text             | nonempty   |
      | command  | command(DoIt)    |            |
      | event    | event(ItWasDone) | How=how    |

      | scenario     | init | handles | issued  |
      | ------------ | ---- | ------- | ------- |
      | "Happy path" |      | event   | command |
      """;
  private static final RootObject POLICY_SCENARIO_OBJECT =
      new RootObject("ape", "acceptanceTest", "BearPolicy")
          .set("sut", new Reference("policy", "Bear"))
          .set(
              "variables",
              new DataList()
                  .add(
                      new NestedObject("how")
                          .set("type", new DataEnum("text"))
                          .set("definitions", new DataList().add(new DataEnum("nonempty"))))
                  .add(new NestedObject("command").set("type", new Reference("command", "DoIt")))
                  .add(
                      new NestedObject("event")
                          .set("type", new Reference("event", "ItWasDone"))
                          .set(
                              "definitions",
                              new DataList()
                                  .add(
                                      new NestedObject("How")
                                          .set("fieldName", new DataString("How"))
                                          .set("value", new Reference("variable", "how"))))))
          .set(
              "scenarios",
              new DataList()
                  .add(
                      new NestedObject("Happy path")
                          .set("handles", new Reference("variable", "event"))
                          .set("issued", new Reference("variable", "command"))));
  private static final String READ_MODEL_SCENARIO =
      """
      | type      | name     |
      | --------- | -------- |
      | readModel | ape.Bear |

      | variable | type             | definition |
      | -------- | ---------------- | ---------- |
      | how      | text             | nonempty   |
      | event    | event(ItWasDone) | How=how    |

      | scenario     | init | handles | state |
      | ------------ | ---- | ------- | ----- |
      | "Happy path" |      | event   |       |
      """;
  private static final RootObject READ_MODEL_SCENARIO_OBJECT =
      new RootObject("ape", "acceptanceTest", "BearReadModel")
          .set("sut", new Reference("readModel", "Bear"))
          .set(
              "variables",
              new DataList()
                  .add(
                      new NestedObject("how")
                          .set("type", new DataEnum("text"))
                          .set("definitions", new DataList().add(new DataEnum("nonempty"))))
                  .add(
                      new NestedObject("event")
                          .set("type", new Reference("event", "ItWasDone"))
                          .set(
                              "definitions",
                              new DataList()
                                  .add(
                                      new NestedObject("How")
                                          .set("fieldName", new DataString("How"))
                                          .set("value", new Reference("variable", "how"))))))
          .set(
              "scenarios",
              new DataList()
                  .add(
                      new NestedObject("Happy path")
                          .set("handles", new Reference("variable", "event"))));
  private final Format format = new AcceptanceFormat();
  private final ByteArrayOutputStream output = new ByteArrayOutputStream();

  @Test
  void shouldParseAggregateScenario() throws IOException {
    output.write(AGGREGATE_SCENARIO.getBytes(UTF_8));

    var actual = format.newParser().parse(new ByteArrayInputStream(output.toByteArray()));

    assertThat(actual).isEqualTo(AGGREGATE_SCENARIO_OBJECT);
  }

  @Test
  void shouldBuildAggregateScenario() throws IOException {
    format.newBuilder().build(AGGREGATE_SCENARIO_OBJECT, output);

    assertThat(output).hasToString(AGGREGATE_SCENARIO);
  }

  @Test
  void shouldParsePolicyScenario() throws IOException {
    output.write(POLICY_SCENARIO.getBytes(UTF_8));

    var actual = format.newParser().parse(new ByteArrayInputStream(output.toByteArray()));

    assertThat(actual).isEqualTo(POLICY_SCENARIO_OBJECT);
  }

  @Test
  void shouldBuildPolicyScenario() throws IOException {
    format.newBuilder().build(POLICY_SCENARIO_OBJECT, output);

    assertThat(output).hasToString(POLICY_SCENARIO);
  }

  @Test
  void shouldParseReadModelScenario() throws IOException {
    output.write(READ_MODEL_SCENARIO.getBytes(UTF_8));

    var actual = format.newParser().parse(new ByteArrayInputStream(output.toByteArray()));

    assertThat(actual).isEqualTo(READ_MODEL_SCENARIO_OBJECT);
  }

  @Test
  void shouldBuildReadModelScenario() throws IOException {
    format.newBuilder().build(READ_MODEL_SCENARIO_OBJECT, output);

    assertThat(output).hasToString(READ_MODEL_SCENARIO);
  }
}
