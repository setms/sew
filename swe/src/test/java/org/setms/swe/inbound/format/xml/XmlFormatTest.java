package org.setms.swe.inbound.format.xml;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.format.DataEnum;
import org.setms.km.domain.model.format.DataList;
import org.setms.km.domain.model.format.NestedObject;
import org.setms.km.domain.model.format.RootObject;

class XmlFormatTest {

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
                                .set("direction", new DataEnum("LEFT_TO_RIGHT"))
                                .set("children", new DataList().add(new NestedObject("Submit"))))));
  }
}
