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
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.format.DataEnum;
import org.setms.km.domain.model.format.DataList;
import org.setms.km.domain.model.format.NestedObject;
import org.setms.km.domain.model.format.RootObject;
import org.setms.swe.domain.model.sdlc.ui.DesignSystem;
import org.setms.swe.domain.model.sdlc.ui.Property;
import org.setms.swe.domain.model.sdlc.ui.Style;
import org.setms.swe.domain.model.sdlc.ux.Affordance;
import org.setms.swe.domain.model.sdlc.ux.Container;
import org.setms.swe.domain.model.sdlc.ux.Direction;
import org.setms.swe.domain.model.sdlc.ux.Wireframe;

class XmlFormatTest {

  @Test
  void shouldRoundTripWireframeWithAffordanceLinkedToCommand() throws IOException {
    var wireframe = givenWireframeWithAffordanceLinkedToCommand();
    var output = new ByteArrayOutputStream();
    new XmlFormat().newBuilder().build(wireframe, output);

    var actual =
        new XmlFormat()
            .newParser()
            .parse("", new ByteArrayInputStream(output.toByteArray()), Wireframe.class, false);

    assertThat(actual)
        .as(
            "Round-tripped wireframe should preserve the affordance's command link"
                + " so that the HTML generator can produce the correct form action")
        .isEqualTo(wireframe);
  }

  private Wireframe givenWireframeWithAffordanceLinkedToCommand() {
    var affordance =
        new Affordance(new FullyQualifiedName("todo", "AddTodoItem"))
            .setCommand(new Link("command", "AddTodoItem"));
    var container =
        new Container(new FullyQualifiedName("todo", "AddTodoItem"))
            .setDirection(Direction.TOP_TO_BOTTOM)
            .setChildren(List.of(affordance));
    return new Wireframe(new FullyQualifiedName("todo", "AddTodoItem"))
        .setContainers(List.of(container));
  }

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
            .parse("", new ByteArrayInputStream(output.toByteArray()), DesignSystem.class, false);

    assertThat(actual)
        .as("Parsed DesignSystem should equal the original, including Style.properties")
        .isEqualTo(expected);
  }

  @Test
  void shouldParseWireframeWithNestedContainers() throws IOException {
    var xml =
        """
        <wireframe package="todo" name="AddTodoItem">
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
            new RootObject("todo", "wireframe", "AddTodoItem")
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
        new RootObject("todo", "wireframe", "AddTodoItem")
            .set(
                "containers",
                new DataList()
                    .add(
                        new NestedObject("AddTodoItem")
                            .setType("container")
                            .set("direction", new DataEnum("top_to_bottom"))
                            .set(
                                "children",
                                new DataList()
                                    .add(
                                        new NestedObject("AddTodoItem")
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
        new RootObject("todo", "wireframe", "AddTodoItem")
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
            "Built XML should parse back to the same RootObject representing 'AddTodoItem' with a 'Header' container")
        .isEqualTo(root);
  }
}
