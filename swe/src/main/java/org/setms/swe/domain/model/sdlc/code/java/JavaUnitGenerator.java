package org.setms.swe.domain.model.sdlc.code.java;

import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.format.Strings.initUpper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.Scenario;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.technology.UnitTestGenerator;
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;

public class JavaUnitGenerator implements UnitTestGenerator {

  @Override
  public List<CodeArtifact> generate(AcceptanceTest acceptanceTest) {
    var className = acceptanceTest.getSut().getId() + "Test";
    var unitTest = new UnitTest(new FullyQualifiedName(acceptanceTest.getPackage(), className));
    unitTest.setCode(generateCode(acceptanceTest.getPackage(), className, acceptanceTest));
    return List.of(unitTest);
  }

  private String generateCode(String packageName, String className, AcceptanceTest acceptanceTest) {
    var builder = new StringBuilder();
    builder.append("package %s;\n".formatted(packageName));
    builder.append("\nimport org.junit.jupiter.api.Test;\n");
    builder.append("\nclass %s {\n".formatted(className));
    for (var scenario : acceptanceTest.getScenarios()) {
      builder.append("\n  @Test\n");
      builder.append("  void %s() {}\n".formatted(toMethodName(scenario)));
    }
    builder.append("}\n");
    return builder.toString();
  }

  private String toMethodName(Scenario scenario) {
    var words = scenario.getName().split("\\s+");
    var result =
        Arrays.stream(words).map(word -> initUpper(initLower(word))).collect(Collectors.joining());
    return initLower(result);
  }
}
