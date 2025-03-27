package org.setms.sew.format.sew;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import org.setms.sew.format.DataList;
import org.setms.sew.format.DataString;
import org.setms.sew.format.Format;
import org.setms.sew.format.NestedObject;
import org.setms.sew.format.Reference;
import org.setms.sew.format.RootObject;
import org.setms.sew.schema.FullyQualifiedName;
import org.setms.sew.schema.SchemaObject;

class SewFormatTest {

  private final Format format = new SewFormat();
  private final ByteArrayOutputStream output = new ByteArrayOutputStream();

  @Test
  void shouldSerializeStringProperties() throws IOException {
    assertSerialization(
        new RootObject("ape", "bear", "cheetah")
            .set("dingo", new DataString("elephant"))
            .set("fox", new DataString("giraffe")),
        """
        package ape

        bear cheetah {
          dingo = "elephant"
          fox = "giraffe"
        }
        """);
  }

  private void assertSerialization(RootObject data, String expected) throws IOException {
    format.newBuilder().build(data, output);

    assertThat(output).hasToString(expected);
  }

  @Test
  void shouldSerializeListProperties() throws IOException {
    assertSerialization(
        new RootObject("hyena", "iguana", "jaguar")
            .set("koala", new DataList().add(new DataString("leopard")))
            .set(
                "mule",
                new DataList().add(new DataString("nightingale"), new DataString("opossum")))
            .set("parrot", new DataList()),
        """
        package hyena

        iguana jaguar {
          koala = [ "leopard" ]
          mule = [
            "nightingale",
            "opossum"
          ]
          parrot = [ ]
        }
        """);
  }

  @Test
  void shouldSerializeNestedObjects() throws IOException {
    assertSerialization(
        new RootObject("rabbit", "snake", "tiger")
            .set("unicorn", new NestedObject("velociraptor").set("weasel", new DataString("zebra")))
            .set(
                "aardvark",
                new DataList()
                    .add(new NestedObject("boar").set("cobra", new DataString("dog")))
                    .add(new NestedObject("eagle").set("gorilla", new DataString("hound")))),
        """
        package rabbit

        snake tiger {
        }

        unicorn velociraptor {
          weasel = "zebra"
        }

        aardvark boar {
          cobra = "dog"
        }

        aardvark eagle {
          gorilla = "hound"
        }
        """);
  }

  @Test
  void shouldSerializeReference() throws IOException {
    assertSerialization(
        new RootObject("lynx", "marmot", "otter").set("panther", new Reference("Rhino")),
        """
        package lynx

        marmot otter {
          panther = Rhino
        }
        """);
  }

  @Test
  void shouldParseObjectWithStringProperties() throws IOException {
    assertDeserialization(
        """
        package ape

        bear cheetah {
          dingo = "elephant"
          fox = "giraffe"
        }
        """,
        new RootObject("ape", "bear", "cheetah")
            .set("dingo", new DataString("elephant"))
            .set("fox", new DataString("giraffe")));
  }

  private void assertDeserialization(String data, RootObject expected) throws IOException {
    output.write(data.getBytes(UTF_8));

    var actual = format.newParser().parse(new ByteArrayInputStream(output.toByteArray()));

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void shouldDeserializeListProperties() throws IOException {
    assertDeserialization(
        """
        package hyena

        iguana jaguar {
          koala = [ "leopard" ]
          mule = [
            "nightingale",
            "opossum"
          ]
          parrot = [ ]
        }
        """,
        new RootObject("hyena", "iguana", "jaguar")
            .set("koala", new DataList().add(new DataString("leopard")))
            .set(
                "mule",
                new DataList().add(new DataString("nightingale"), new DataString("opossum")))
            .set("parrot", new DataList()));
  }

  @Test
  void shouldDeserializeNestedObjects() throws IOException {
    assertDeserialization(
        """
        package rabbit

        snake tiger {
        }

        unicorn velociraptor {
          weasel = "zebra"
        }

        aardvark boar {
          cobra = "dog"
        }

        aardvark eagle {
          gorilla = "hound"
        }
        """,
        new RootObject("rabbit", "snake", "tiger")
            .set("unicorn", new NestedObject("velociraptor").set("weasel", new DataString("zebra")))
            .set(
                "aardvark",
                new DataList()
                    .add(new NestedObject("boar").set("cobra", new DataString("dog")))
                    .add(new NestedObject("eagle").set("gorilla", new DataString("hound")))));
  }

  @Test
  void shouldDeserializeReference() throws IOException {
    assertDeserialization(
        """
        package lynx

        marmot otter {
          panther = Rhino
        }
        """,
        new RootObject("lynx", "marmot", "otter").set("panther", new Reference("Rhino")));
  }

  @Test
  void shouldConvertToObject() {
    var actual =
        format
            .newParser()
            .convert(
                new RootObject("ape", "Bear", "cheetah").set("dingo", new DataString("elephant")),
                Bear.class);

    assertThat(actual)
        .isEqualTo(new Bear(new FullyQualifiedName("ape.cheetah")).setDingo("elephant"));
  }

  @Getter
  @Setter
  @Accessors(chain = true)
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  public static class Bear extends SchemaObject {

    private String dingo;

    public Bear(FullyQualifiedName fullyQualifiedName) {
      super(fullyQualifiedName);
    }
  }
}
