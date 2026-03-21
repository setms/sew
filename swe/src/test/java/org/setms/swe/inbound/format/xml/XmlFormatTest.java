package org.setms.swe.inbound.format.xml;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.format.DataEnum;
import org.setms.km.domain.model.format.DataList;
import org.setms.km.domain.model.format.NestedObject;
import org.setms.km.domain.model.format.RootObject;
import org.setms.swe.domain.model.sdlc.ui.DesignSystem;
import org.setms.swe.domain.model.sdlc.ui.Property;
import org.setms.swe.domain.model.sdlc.ui.Style;

class XmlFormatTest {

  @Test
  void shouldRoundTripDesignSystemWithStyleProperties() throws IOException {
    var property = new Property(new FullyQualifiedName("ux.ButtonFontSize")).setValue("14px");
    var style = new Style(new FullyQualifiedName("ux.Default")).setProperties(List.of(property));
    var expected = new DesignSystem(new FullyQualifiedName("ux.Default")).setStyles(List.of(style));
    var output = new ByteArrayOutputStream();

    new XmlFormat().newBuilder().build(expected, output);
    var actual =
        new XmlFormat()
            .newParser()
            .parse(new ByteArrayInputStream(output.toByteArray()), DesignSystem.class, false);

    assertThat(actual)
        .as("Parsed DesignSystem should equal the original, including Style.properties")
        .isEqualTo(expected);
  }

  @Test
  void shouldParseWireframeWithNestedContainers() throws IOException {
    var xml =
        """
        <wireframe package="todo" name="InitiateAddTodoItem">
          <container name="Header" direction="LEFT_TO_RIGHT">
            <children>
              <affordance name="Submit"/>
            </children>
          </container>
        </wireframe>
        """;

    var actual = new XmlFormat().newParser().parse(new ByteArrayInputStream(xml.getBytes(UTF_8)));

    assertThat(actual)
        .as(
            "Parsed wireframe should have container 'Header' with direction and an affordance child")
        .isEqualTo(
            new RootObject("todo", "wireframe", "InitiateAddTodoItem")
                .set(
                    "containers",
                    new DataList()
                        .add(
                            new NestedObject("Header")
                                .setType("container")
                                .set("direction", new DataEnum("LEFT_TO_RIGHT"))
                                .set(
                                    "children",
                                    new DataList()
                                        .add(new NestedObject("Submit").setType("affordance"))))));
  }

  @Test
  void shouldRoundTripWireframeWithTypedChildren() throws IOException {
    var root =
        new RootObject("todo", "wireframe", "InitiateAddTodoItem")
            .set(
                "containers",
                new DataList()
                    .add(
                        new NestedObject("InitiateAddTodoItem")
                            .setType("container")
                            .set("direction", new DataEnum("top_to_bottom"))
                            .set(
                                "children",
                                new DataList()
                                    .add(
                                        new NestedObject("InitiateAddTodoItem")
                                            .setType("affordance")
                                            .set(
                                                "inputFields",
                                                new DataList()
                                                    .add(
                                                        new NestedObject("Task")
                                                            .setType("inputField")
                                                            .set(
                                                                "type", new DataEnum("text"))))))));
    var output = new ByteArrayOutputStream();

    new XmlFormat().newBuilder().build(root, new PrintWriter(output, true));
    var actual = new XmlFormat().newParser().parse(new ByteArrayInputStream(output.toByteArray()));

    assertThat(actual)
        .as(
            "Round-tripped wireframe must preserve the type of children (affordance) and their children (inputField)")
        .isEqualTo(root);
  }

  @Test
  void shouldBuildWireframeWithContainers() throws IOException {
    var root =
        new RootObject("todo", "wireframe", "InitiateAddTodoItem")
            .set(
                "containers",
                new DataList()
                    .add(
                        new NestedObject("Header")
                            .setType("container")
                            .set("direction", new DataEnum("LEFT_TO_RIGHT"))));
    var output = new ByteArrayOutputStream();

    new XmlFormat().newBuilder().build(root, new PrintWriter(output, true));

    var actual = new XmlFormat().newParser().parse(new ByteArrayInputStream(output.toByteArray()));
    assertThat(actual)
        .as(
            "Built XML should parse back to the same RootObject representing 'InitiateAddTodoItem' with a 'Header' container")
        .isEqualTo(root);
  }
}
