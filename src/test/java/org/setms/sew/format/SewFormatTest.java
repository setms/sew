package org.setms.sew.format;

import org.junit.jupiter.api.Test;
import org.setms.sew.format.sew.SewFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SewFormatTest {

  private final Format format = new SewFormat();
  private final OutputStream output = new ByteArrayOutputStream();

  @Test
  void shouldSerializeStringProperties() throws IOException {
    assertSerialization(new RootObject("ape", "bear", "cheetah")
        .set("dingo", new DataString("elephant"))
        .set("fox", new DataString("giraffe")), """
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
    assertSerialization(new RootObject("hyena", "iguana", "jaguar")
        .set("koala", new DataList().add(new DataString("leopard")))
        .set("mule", new DataList().add(new DataString("nightingale"), new DataString("opossum")))
        .set("parrot", new DataList()), """
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
    assertSerialization(new RootObject("rabbit", "snake", "tiger")
        .set("unicorn", new NestedObject("velociraptor").set("weasel", new DataString("zebra")))
        .set("aardvark", new DataList().add(
            new NestedObject("boar").set("cobra", new DataString("dog"))
        ).add(new NestedObject("eagle").set("gorilla", new DataString("hound")))), """
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
    assertSerialization(new RootObject("lynx", "marmot", "otter")
        .set("panther", new Reference("Rhino")), """
        package lynx
        
        marmot otter {
          panther = Rhino
        }
        """);
  }

}
