package org.setms.km.domain.model.artifact;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.setms.km.test.MainArtifact;

class LinkTest {

  @Test
  void shouldLinkToArtifact() {
    var artifact = new MainArtifact(new FullyQualifiedName("ape.Bear"));
    var otherArtifact = new MainArtifact(new FullyQualifiedName("ape.Cheetah"));

    var actual = new Link(artifact);

    assertThat(actual.getType()).as("Type").isEqualTo("mainArtifact");
    assertThat(actual.hasType("mainArtifact")).as("HasType").isTrue();
    assertThat(actual.getId()).as("Id").isEqualTo("Bear");
    assertThat(actual.getAttributes()).as("Attributes").isEmpty();
    assertThat(actual.optAttribute("attribute")).as("Attribute").isEmpty();
    assertThat(actual.pointsTo(artifact)).as("Points to").isTrue();
    assertThat(actual.pointsTo(otherArtifact)).as("Points to other").isFalse();
    assertThat(actual.resolveFrom(List.of(otherArtifact, artifact)))
        .as("Resolvable")
        .isPresent()
        .hasValue(artifact);
    assertThat(actual.resolveFrom(List.of(otherArtifact))).as("Unresolvable").isEmpty();
  }

  @ParameterizedTest
  @CsvSource(
      nullValues = "null",
      value = {
        "null,ape,null,ape,0",
        "null,bear,null,cheetah,-1",
        "null,elephant,null,dingo,1",
        "null,fox,giraffe,fox,-1",
        "hyena,iguana,null,iguana,1",
        "jaguar,koala,jaguar,leopard,-1",
        "mule,opossum,mule,nightingale,1",
      })
  void shouldCompareLinks(String type1, String id1, String type2, String id2, int expected) {
    var link1 = new Link(type1, id1);
    var link2 = new Link(type2, id2);

    var actual = link1.compareTo(link2);

    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource(
      nullValues = "null",
      value = {"null,ape,ape", "bear,cheetah,bear(cheetah)"})
  void shouldConvertToString(String type, String id, String expected) {
    var actual = new Link(type, id);

    assertThat(actual).hasToString(expected);
  }

  @Test
  void shouldTestForEquality() {
    var link = new Link("ape", "bear");
    var other = new Link("ape", "cheetah");

    assertThat(link.testEqual().test(link)).isTrue();
    assertThat(link.testEqual().test(other)).isFalse();
  }

  @Test
  void shouldTestForType() {
    var link = new Link("ape", "bear");
    var other = new Link("cheetah", "dingo");

    var tester = Link.testType("ape");

    assertThat(tester.test(link)).isTrue();
    assertThat(tester.test(other)).isFalse();
  }
}
