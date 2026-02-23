package org.setms.km.domain.model.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GlobTest {

  @ParameterizedTest
  @CsvSource({
    "ape,bear,cheetah,false",
    "ape,**/*.bear,ape/cheetah.bear,true",
    "ape/bear,**/.cheetah,/dingo/ape/bear/elephant.cheetah,true",
    "ape/bear,**/.cheetah,/dingo/ape/bear/elephant/fox.cheetah,true",
    "src/test/java,**/*Test.java,src/test/java/com/company/todo/TodoItemsTest.java,true",
    "src/test/java,**/*Test.java,src/test/java/com/company/todo/TestDataBuilder.java,false",
    "src/test/java,**/*Test.java,/project/src/test/java/TodoItemsTest.java,true",
    "src/test/java,**/*Test.java,/project/src/test/java/TestDataBuilder.java,false",
    "src/test/java,**/*Test.java,/project/src/test/java/nested/MyTest.java,true",
    "src/test/java,**/*Test.java,/project/src/test/java/nested/MyTestData.java,false",
    "src,**/*Config.xml,src/main/resources/app/DatabaseConfig.xml,true",
    "src,**/*Config.xml,src/main/resources/app/ConfigHelper.xml,false"
  })
  void shouldMatch(String path, String pattern, String candidate, boolean expected) {
    var actual = new Glob(path, pattern).matches(candidate);

    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
    "**/*.java,java",
    "**/*Test.java,java",
    "**/Test*.xml,xml",
    "**/*.properties,properties",
    "*Config.yml,yml"
  })
  void shouldExtractExtension(String pattern, String expected) {
    var actual = new Glob("src", pattern).extension();

    assertThat(actual).isEqualTo(expected);
  }
}
