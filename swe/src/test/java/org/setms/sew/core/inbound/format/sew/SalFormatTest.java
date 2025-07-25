package org.setms.sew.core.inbound.format.sew;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.format.DataEnum;
import org.setms.km.domain.model.format.DataList;
import org.setms.km.domain.model.format.DataString;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.format.NestedObject;
import org.setms.km.domain.model.format.Reference;
import org.setms.km.domain.model.format.RootObject;
import org.setms.sew.core.inbound.format.sal.SalFormat;

class SalFormatTest {

  private final Format format = new SalFormat();
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

        aardvark boar {
          cobra = "dog"
        }

        aardvark eagle {
          gorilla = "hound"
        }

        unicorn velociraptor {
          weasel = "zebra"
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
  void shouldSerializeEnum() throws IOException {
    assertSerialization(
        new RootObject("lynx", "marmot", "otter").set("panther", new DataEnum("rhino")),
        """
        package lynx

        marmot otter {
          panther = rhino
        }
        """);
  }

  @Test
  void shouldParseObjectWithStringProperties() throws IOException {
    assertDeserialization(
        """
        package ape

        decision Cheetah {
          dingo = "elephant"
          fox = "giraffe"
        }
        """,
        new RootObject("ape", "decision", "Cheetah")
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

        command Jaguar {
          koala = [ "leopard" ]
          mule = [
            "nightingale",
            "opossum"
          ]
          parrot = [ ]
        }
        """,
        new RootObject("hyena", "command", "Jaguar")
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

        useCase Tiger {
        }

        scenario Velociraptor {
          weasel = "zebra"
        }

        user Boar {
          cobra = "dog"
        }

        user Eagle {
          gorilla = "hound"
        }
        """,
        new RootObject("rabbit", "useCase", "Tiger")
            .set(
                "scenario", new NestedObject("Velociraptor").set("weasel", new DataString("zebra")))
            .set(
                "user",
                new DataList()
                    .add(new NestedObject("Boar").set("cobra", new DataString("dog")))
                    .add(new NestedObject("Eagle").set("gorilla", new DataString("hound")))));
  }

  @Test
  void shouldDeserializeReference() throws IOException {
    assertDeserialization(
        """
        package lynx

        event Otter {
          panther = Rhino
        }
        """,
        new RootObject("lynx", "event", "Otter").set("panther", new Reference("Rhino")));
  }

  @Test
  void shouldDeserializeEnum() throws IOException {
    assertDeserialization(
        """
        package lynx

        event Otter {
          panther = rhino
        }
        """,
        new RootObject("lynx", "event", "Otter").set("panther", new DataEnum("rhino")));
  }

  @Test
  void shouldParseDomainObject() {
    var actual =
        format
            .newParser()
            .convert(
                new RootObject("ape", "bear", "cheetah")
                    .set("dingo", new DataString("elephant"))
                    .set("fox", new DataList().add(new DataString("giraffe")))
                    .set(
                        "hyenas",
                        new DataList()
                            .add(new NestedObject("iguana").set("jaguar", new DataString("koala"))))
                    .set("leopard", new Reference("mule")),
                Bear.class,
                false);

    assertThat(actual)
        .isEqualTo(
            new Bear(new FullyQualifiedName("ape.cheetah"))
                .setDingo("elephant")
                .setFox(List.of("giraffe"))
                .setHyenas(
                    List.of(new Hyena(new FullyQualifiedName("hyenas.iguana")).setJaguar("koala")))
                .setLeopard(new Link(null, "mule")));
  }

  @Test
  void shouldParseSubObjects() {
    var actual =
        format
            .newParser()
            .convert(
                new RootObject("ape", "bear", "cheetah")
                    .set("dingo", new DataString("elephant"))
                    .set("fox", new DataList().add(new DataString("giraffe")))
                    .set(
                        "hyenas", new NestedObject("iguana").set("jaguar", new DataString("koala")))
                    .set("leopard", new Reference("mule"))
                    .set("ok", new DataEnum("true"))
                    .set("state", new DataEnum("sucks")),
                Bear.class,
                false);

    assertThat(actual)
        .isEqualTo(
            new Bear(new FullyQualifiedName("ape.cheetah"))
                .setDingo("elephant")
                .setFox(List.of("giraffe"))
                .setHyenas(
                    List.of(new Hyena(new FullyQualifiedName("hyenas.iguana")).setJaguar("koala")))
                .setLeopard(new Link(null, "mule"))
                .setOk(true)
                .setState(State.SUCKS));
  }

  @Test
  void shouldBuildDomainObject() {
    var actual =
        format
            .newBuilder()
            .toRootObject(
                new Bear(new FullyQualifiedName("ape.cheetah"))
                    .setDingo("elephant")
                    .setFox(List.of("giraffe"))
                    .setHyenas(
                        List.of(
                            new Hyena(new FullyQualifiedName("hyena.iguana")).setJaguar("koala")))
                    .setLeopard(new Link(null, "mule"))
                    .setOk(true)
                    .setState(State.SORTA_OK));

    assertThat(actual)
        .isEqualTo(
            new RootObject("ape", "bear", "cheetah")
                .set("dingo", new DataString("elephant"))
                .set("fox", new DataList().add(new DataString("giraffe")))
                .set(
                    "hyenas",
                    new DataList()
                        .add(new NestedObject("iguana").set("jaguar", new DataString("koala"))))
                .set("leopard", new Reference("mule"))
                .set("ok", new DataEnum("true"))
                .set("state", new DataEnum("sorta_ok")));
  }
}
