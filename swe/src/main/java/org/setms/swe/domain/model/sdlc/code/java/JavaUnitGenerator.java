package org.setms.swe.domain.model.sdlc.code.java;

import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.format.Strings.initUpper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.AggregateScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ElementVariable;
import org.setms.swe.domain.model.sdlc.acceptancetest.FieldVariable;
import org.setms.swe.domain.model.sdlc.acceptancetest.PolicyScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ReadModelScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.Scenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.Variable;
import org.setms.swe.domain.model.sdlc.technology.UnitTestGenerator;
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;

public class JavaUnitGenerator implements UnitTestGenerator {

  @Override
  public UnitTest generate(AcceptanceTest acceptanceTest) {
    var className = acceptanceTest.getSut().getId() + "Test";
    var result = new UnitTest(new FullyQualifiedName(acceptanceTest.getPackage(), className));
    result.setCode(generateCode(acceptanceTest.getPackage(), className, acceptanceTest));
    return result;
  }

  private String generateCode(String packageName, String className, AcceptanceTest acceptanceTest) {
    var builder = new StringBuilder();
    builder.append("package %s;\n".formatted(packageName));
    if (needsAssertThat(acceptanceTest)) {
      builder.append("\nimport static org.assertj.core.api.Assertions.assertThat;\n");
    }
    builder.append("\nimport org.junit.jupiter.api.Test;\n");
    builder.append("\nclass %s {\n".formatted(className));
    for (var scenario : acceptanceTest.getScenarios()) {
      builder.append("\n  @Test\n");
      builder.append("  void %s() {\n".formatted(toMethodName(scenario)));
      generateMethodBody(scenario, acceptanceTest, builder);
      builder.append("  }\n");
    }
    builder.append("}\n");
    return builder.toString();
  }

  private boolean needsAssertThat(AcceptanceTest acceptanceTest) {
    return acceptanceTest.getScenarios().stream()
        .anyMatch(
            scenario ->
                switch (scenario) {
                  case AggregateScenario s -> s.getEmitted() != null;
                  case PolicyScenario s -> s.getIssued() != null;
                  default -> false;
                });
  }

  private void generateMethodBody(
      Scenario scenario, AcceptanceTest acceptanceTest, StringBuilder builder) {
    switch (scenario) {
      case AggregateScenario s -> generateAggregateBody(s, acceptanceTest, builder);
      case PolicyScenario s -> generatePolicyBody(s, acceptanceTest, builder);
      case ReadModelScenario s -> generateReadModelBody(s, acceptanceTest, builder);
      default -> {}
    }
  }

  private void generateAggregateBody(
      AggregateScenario scenario, AcceptanceTest acceptanceTest, StringBuilder builder) {
    var commandVar = resolveElementVariable(scenario.getAccepts(), acceptanceTest);
    var emittedVar = resolveElementVariable(scenario.getEmitted(), acceptanceTest);
    var fieldVars = collectFieldVariables(commandVar, emittedVar, acceptanceTest);
    generateFieldDeclarations(fieldVars, builder);
    generateElementConstruction(commandVar, builder);
    builder.append("\n");
    var sutName = acceptanceTest.getSut().getId();
    builder.append(
        "    var actual = new %s().accept(%s);\n".formatted(sutName, commandVar.getName()));
    builder.append("\n");
    if (emittedVar != null) {
      generateAssertEquals(emittedVar, builder);
    }
  }

  private ElementVariable resolveElementVariable(Link link, AcceptanceTest acceptanceTest) {
    if (link == null) {
      return null;
    }
    return acceptanceTest
        .findVariable(link)
        .filter(ElementVariable.class::isInstance)
        .map(ElementVariable.class::cast)
        .orElse(null);
  }

  private Map<String, FieldVariable> collectFieldVariables(
      ElementVariable inputVar, ElementVariable outputVar, AcceptanceTest acceptanceTest) {
    var result = new LinkedHashMap<String, FieldVariable>();
    collectFieldVariablesFrom(inputVar, acceptanceTest, result);
    collectFieldVariablesFrom(outputVar, acceptanceTest, result);
    return result;
  }

  private void collectFieldVariablesFrom(
      ElementVariable elementVar,
      AcceptanceTest acceptanceTest,
      Map<String, FieldVariable> target) {
    if (elementVar == null || elementVar.getDefinitions() == null) {
      return;
    }
    for (var assignment : elementVar.getDefinitions()) {
      acceptanceTest
          .findVariable(assignment.getValue())
          .filter(FieldVariable.class::isInstance)
          .map(FieldVariable.class::cast)
          .ifPresent(fieldVar -> target.putIfAbsent(fieldVar.getName(), fieldVar));
    }
  }

  private void generateFieldDeclarations(
      Map<String, FieldVariable> fieldVars, StringBuilder builder) {
    fieldVars.forEach(
        (name, fieldVar) ->
            builder.append(
                "    var %s = TestData.%s();\n".formatted(name, testDataMethodFor(fieldVar))));
  }

  private static final Map<String, String> TEST_DATA_METHODS =
      Map.of(
          "text", "someText",
          "number", "someNumber",
          "boolean", "someBoolean",
          "date", "someDate",
          "time", "someTime",
          "datetime", "someDateTime",
          "id", "someId",
          "selection", "someSelection");

  @SuppressWarnings("rawtypes")
  private String testDataMethodFor(FieldVariable fieldVar) {
    var typeName = ((Variable) fieldVar).getType().toString().toLowerCase();
    return TEST_DATA_METHODS.getOrDefault(typeName, "some" + initUpper(typeName));
  }

  private void generateElementConstruction(ElementVariable elementVar, StringBuilder builder) {
    var typeName = elementVar.getType().getId();
    builder.append(
        "    var %s = new %s()%s;\n"
            .formatted(elementVar.getName(), typeName, chainedSetters(elementVar)));
  }

  private String chainedSetters(ElementVariable elementVar) {
    if (elementVar.getDefinitions() == null || elementVar.getDefinitions().isEmpty()) {
      return "";
    }
    var result = new StringBuilder();
    for (var assignment : elementVar.getDefinitions()) {
      var valueName = assignment.getValue().getId();
      result.append(".set%s(%s)".formatted(assignment.getFieldName(), valueName));
    }
    return result.toString();
  }

  private void generateAssertEquals(ElementVariable elementVar, StringBuilder builder) {
    var typeName = elementVar.getType().getId();
    builder.append(
        "    assertThat(actual).isEqualTo(new %s()%s);\n"
            .formatted(typeName, chainedSetters(elementVar)));
  }

  private void generatePolicyBody(
      PolicyScenario scenario, AcceptanceTest acceptanceTest, StringBuilder builder) {
    var handlesVar = resolveElementVariable(scenario.getHandles(), acceptanceTest);
    var issuedVar = resolveElementVariable(scenario.getIssued(), acceptanceTest);
    var fieldVars = collectFieldVariables(handlesVar, issuedVar, acceptanceTest);
    generateFieldDeclarations(fieldVars, builder);
    generateElementConstruction(handlesVar, builder);
    builder.append("\n");
    var sutName = acceptanceTest.getSut().getId();
    builder.append(
        "    var actual = new %s().handle(%s);\n".formatted(sutName, handlesVar.getName()));
    builder.append("\n");
    if (issuedVar != null) {
      generateAssertEquals(issuedVar, builder);
    }
  }

  private void generateReadModelBody(
      ReadModelScenario scenario, AcceptanceTest acceptanceTest, StringBuilder builder) {
    var handlesVar = resolveElementVariable(scenario.getHandles(), acceptanceTest);
    var fieldVars = collectFieldVariables(handlesVar, null, acceptanceTest);
    generateFieldDeclarations(fieldVars, builder);
    generateElementConstruction(handlesVar, builder);
    builder.append("\n");
    var sutName = acceptanceTest.getSut().getId();
    builder.append("    new %s().handle(%s);\n".formatted(sutName, handlesVar.getName()));
  }

  private String toMethodName(Scenario scenario) {
    var words = scenario.getName().split("\\s+");
    var result =
        Arrays.stream(words).map(word -> initUpper(initLower(word))).collect(Collectors.joining());
    return initLower(result);
  }
}
