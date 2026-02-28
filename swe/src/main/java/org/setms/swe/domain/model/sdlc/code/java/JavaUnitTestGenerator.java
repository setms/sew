package org.setms.swe.domain.model.sdlc.code.java;

import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.format.Strings.initUpper;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.acceptancetest.AggregateScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ElementVariable;
import org.setms.swe.domain.model.sdlc.acceptancetest.FieldAssignment;
import org.setms.swe.domain.model.sdlc.acceptancetest.FieldVariable;
import org.setms.swe.domain.model.sdlc.acceptancetest.PolicyScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.ReadModelScenario;
import org.setms.swe.domain.model.sdlc.acceptancetest.Scenario;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.technology.UnitTestGenerator;
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;

@RequiredArgsConstructor
public class JavaUnitTestGenerator extends JavaArtifactGenerator implements UnitTestGenerator {

  private final String topLevelPackage;

  public static Optional<UnitTestGenerator> from(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    return topLevelPackage(inputs, diagnostics).map(JavaUnitTestGenerator::new);
  }

  @Override
  public List<CodeArtifact> generate(AcceptanceTest acceptanceTest) {
    var className = "%sTest".formatted(acceptanceTest.getName());
    var packageName =
        topLevelPackage.endsWith(acceptanceTest.getPackage())
            ? topLevelPackage
            : topLevelPackage + "." + acceptanceTest.getPackage();
    var unitTest = new UnitTest(new FullyQualifiedName(packageName, className));
    unitTest.setCode(generateCode(packageName, className, acceptanceTest));
    var testDataBuilder = new CodeArtifact(new FullyQualifiedName(packageName, "TestDataBuilder"));
    testDataBuilder.setCode(testDataBuilderCode(testDataBuilder, acceptanceTest));
    return List.of(unitTest, testDataBuilder);
  }

  private String generateCode(String packageName, String className, AcceptanceTest acceptanceTest) {
    var builder = new StringBuilder();
    builder.append("package %s;\n".formatted(packageName));
    generateImports(packageName, acceptanceTest, builder);
    builder.append("\nclass %s {\n".formatted(className));
    generateServiceField(acceptanceTest, builder);
    for (var scenario : acceptanceTest.getScenarios()) {
      builder.append("\n  @Test\n");
      builder.append("  void %s() {\n".formatted(toMethodName(scenario)));
      generateMethodBody(scenario, acceptanceTest, builder);
      builder.append("  }\n");
    }
    builder.append("}\n");
    return formatCode(builder.toString());
  }

  private String formatCode(String code) {
    return code.lines().map(this::formatLine).collect(joining("\n")) + "\n";
  }

  private String formatLine(String line) {
    if (line.length() <= 100) {
      return line;
    }
    var assignmentIndex = line.indexOf(" = ");
    if (assignmentIndex < 0) {
      return line;
    }
    var indent = " ".repeat(indentOf(line) + 4);
    return line.substring(0, assignmentIndex + 2)
        + "\n"
        + indent
        + line.substring(assignmentIndex + 3);
  }

  private int indentOf(String line) {
    var result = 0;
    while (result < line.length() && line.charAt(result) == ' ') {
      result++;
    }
    return result;
  }

  private void generateImports(
      String packageName, AcceptanceTest acceptanceTest, StringBuilder builder) {
    var regularImports = new TreeSet<String>();
    var staticImports = new TreeSet<String>();
    var sutName = acceptanceTest.getSut().getId();
    regularImports.add("%s.domain.services.%sService".formatted(packageName, sutName));
    for (var scenario : acceptanceTest.getScenarios()) {
      collectModelImports(packageName, scenario, acceptanceTest, regularImports);
    }
    for (var inputVar : collectInputVariables(acceptanceTest)) {
      staticImports.add(
          "%s.TestDataBuilder.some%s".formatted(packageName, inputVar.getType().getId()));
    }
    if (regularImports.stream().anyMatch(i -> i.startsWith(packageName + ".domain.model."))) {
      staticImports.add("org.assertj.core.api.Assertions.assertThat");
    }
    regularImports.add("org.junit.jupiter.api.Test");
    builder.append("\n");
    for (var imp : staticImports) {
      builder.append("import static %s;\n".formatted(imp));
    }
    builder.append("\n");
    for (var imp : regularImports) {
      builder.append("import %s;\n".formatted(imp));
    }
  }

  private void collectModelImports(
      String packageName,
      Scenario scenario,
      AcceptanceTest acceptanceTest,
      TreeSet<String> imports) {
    switch (scenario) {
      case AggregateScenario s ->
          addModelImport(packageName, s.getEmitted(), acceptanceTest, imports);
      case PolicyScenario s -> addModelImport(packageName, s.getIssued(), acceptanceTest, imports);
      default -> {}
    }
  }

  private void addModelImport(
      String packageName, Link link, AcceptanceTest acceptanceTest, TreeSet<String> imports) {
    var elementVar = resolveElementVariable(link, acceptanceTest);
    if (elementVar != null) {
      imports.add("%s.domain.model.%s".formatted(packageName, elementVar.getType().getId()));
    }
  }

  private void generateServiceField(AcceptanceTest acceptanceTest, StringBuilder builder) {
    var sutName = acceptanceTest.getSut().getId();
    builder.append(
        "\n  private final %sService service = new %sService();\n".formatted(sutName, sutName));
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
    generateSomeCall(commandVar, builder);
    if (emittedVar != null) {
      generateExpected(emittedVar, commandVar, builder);
    }
    builder.append("\n");
    builder.append("    var actual = service.accept(%s);\n".formatted(commandVar.getName()));
    builder.append("\n");
    if (emittedVar != null) {
      builder.append("    assertThat(actual).isEqualTo(expected);\n");
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

  private void generateSomeCall(ElementVariable elementVar, StringBuilder builder) {
    var typeName = elementVar.getType().getId();
    builder.append("    var %s = some%s();\n".formatted(elementVar.getName(), typeName));
  }

  private void generateExpected(
      ElementVariable outputVar, ElementVariable inputVar, StringBuilder builder) {
    var typeName = outputVar.getType().getId();
    builder.append(
        "    var expected = new %s()%s;\n"
            .formatted(typeName, expectedSetters(outputVar, inputVar)));
  }

  private String expectedSetters(ElementVariable outputVar, ElementVariable inputVar) {
    if (outputVar.getDefinitions() == null || outputVar.getDefinitions().isEmpty()) {
      return "";
    }
    var result = new StringBuilder();
    for (var assignment : outputVar.getDefinitions()) {
      var inputFieldName = findMatchingFieldName(assignment.getValue(), inputVar);
      result.append(
          ".set%s(%s.get%s())"
              .formatted(assignment.getFieldName(), inputVar.getName(), inputFieldName));
    }
    return result.toString();
  }

  private String findMatchingFieldName(Link variableRef, ElementVariable inputVar) {
    return inputVar.getDefinitions().stream()
        .filter(a -> a.getValue().getId().equals(variableRef.getId()))
        .map(FieldAssignment::getFieldName)
        .findFirst()
        .orElse(initUpper(variableRef.getId()));
  }

  private void generatePolicyBody(
      PolicyScenario scenario, AcceptanceTest acceptanceTest, StringBuilder builder) {
    var handlesVar = resolveElementVariable(scenario.getHandles(), acceptanceTest);
    var issuedVar = resolveElementVariable(scenario.getIssued(), acceptanceTest);
    generateSomeCall(handlesVar, builder);
    if (issuedVar != null) {
      generateExpected(issuedVar, handlesVar, builder);
    }
    builder.append("\n");
    builder.append("    var actual = service.handle(%s);\n".formatted(handlesVar.getName()));
    builder.append("\n");
    if (issuedVar != null) {
      builder.append("    assertThat(actual).isEqualTo(expected);\n");
    }
  }

  private void generateReadModelBody(
      ReadModelScenario scenario, AcceptanceTest acceptanceTest, StringBuilder builder) {
    var handlesVar = resolveElementVariable(scenario.getHandles(), acceptanceTest);
    generateSomeCall(handlesVar, builder);
    builder.append("\n");
    builder.append("    service.handle(%s);\n".formatted(handlesVar.getName()));
  }

  private String toMethodName(Scenario scenario) {
    var words = scenario.getName().split("\\s+");
    var result = Arrays.stream(words).map(word -> initUpper(initLower(word))).collect(joining());
    return initLower(result);
  }

  private String testDataBuilderCode(CodeArtifact code, AcceptanceTest acceptanceTest) {
    var builder = new StringBuilder();
    var inputVars = collectInputVariables(acceptanceTest);
    builder.append("package %s;\n".formatted(code.getPackage()));
    addTestDataBuilderImports(code.getPackage(), inputVars, acceptanceTest, builder);
    builder.append("\n@NoArgsConstructor(access = AccessLevel.PRIVATE)\n");
    builder.append("public class ").append(code.getName()).append(" {\n");
    for (var inputVar : inputVars) {
      generateFactoryMethods(inputVar, acceptanceTest, builder);
    }
    builder.append("}\n");
    return builder.toString();
  }

  private List<ElementVariable> collectInputVariables(AcceptanceTest acceptanceTest) {
    return acceptanceTest.getScenarios().stream()
        .map(
            scenario ->
                switch (scenario) {
                  case AggregateScenario s -> s.getAccepts();
                  case PolicyScenario s -> s.getHandles();
                  case ReadModelScenario s -> s.getHandles();
                  default -> (Link) null;
                })
        .map(link -> resolveElementVariable(link, acceptanceTest))
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  private void addTestDataBuilderImports(
      String packageName,
      List<ElementVariable> inputVars,
      AcceptanceTest acceptanceTest,
      StringBuilder builder) {
    var imports = new TreeSet<String>();
    for (var inputVar : inputVars) {
      imports.add("%s.domain.model.%s".formatted(packageName, inputVar.getType().getId()));
    }
    var fieldVars = resolveAllFieldVariables(inputVars, acceptanceTest);
    if (fieldVars.stream().anyMatch(f -> f.getType() == FieldType.DATETIME)) {
      imports.add("java.time.OffsetDateTime");
    }
    imports.add("lombok.AccessLevel");
    imports.add("lombok.NoArgsConstructor");
    imports.add("net.jqwik.api.Arbitraries");
    imports.add("net.jqwik.api.Arbitrary");
    imports.add("net.jqwik.api.Combinators");
    builder.append("\n");
    for (var imp : imports) {
      builder.append("import %s;\n".formatted(imp));
    }
  }

  private List<FieldVariable> resolveAllFieldVariables(
      List<ElementVariable> inputVars, AcceptanceTest acceptanceTest) {
    return inputVars.stream()
        .flatMap(v -> resolveFieldVariablesOf(v, acceptanceTest).stream())
        .distinct()
        .toList();
  }

  private List<FieldVariable> resolveFieldVariablesOf(
      ElementVariable inputVar, AcceptanceTest acceptanceTest) {
    return inputVar.getDefinitions().stream()
        .map(FieldAssignment::getValue)
        .map(acceptanceTest::findVariable)
        .flatMap(java.util.Optional::stream)
        .filter(FieldVariable.class::isInstance)
        .map(FieldVariable.class::cast)
        .toList();
  }

  private void generateFactoryMethods(
      ElementVariable inputVar, AcceptanceTest acceptanceTest, StringBuilder builder) {
    var typeName = inputVar.getType().getId();
    var varName = inputVar.getName();
    builder.append("\n  public static %s some%s() {\n".formatted(typeName, typeName));
    builder.append("    return %ss().sample();\n".formatted(varName));
    builder.append("  }\n");
    var fieldVars = resolveFieldVariablesOf(inputVar, acceptanceTest);
    builder.append("\n  private static Arbitrary<%s> %ss() {\n".formatted(typeName, varName));
    builder.append("    return Combinators.combine(");
    for (var i = 0; i < fieldVars.size(); i++) {
      var separator = i < fieldVars.size() - 1 ? ", " : "";
      builder.append("%ss()%s".formatted(fieldVars.get(i).getName(), separator));
    }
    builder.append(").as(%s::new);\n".formatted(typeName));
    builder.append("  }\n");
    for (var fieldVar : fieldVars) {
      generateFieldArbitraryMethod(fieldVar, builder);
    }
  }

  private void generateFieldArbitraryMethod(FieldVariable fieldVar, StringBuilder builder) {
    var javaType = javaType(fieldVar.getType());
    builder.append(
        "\n  private static Arbitrary<%s> %ss() {\n".formatted(javaType, fieldVar.getName()));
    builder.append("    return %s;\n".formatted(arbitraryExpression(fieldVar)));
    builder.append("  }\n");
  }

  private String javaType(FieldType type) {
    return switch (type) {
      case TEXT -> "Text";
      case DATETIME -> "OffsetDateTime";
      default -> type.name();
    };
  }

  private String arbitraryExpression(FieldVariable fieldVar) {
    return switch (fieldVar.getType()) {
      case TEXT -> textArbitrary(fieldVar.getDefinitions());
      case DATETIME -> "Arbitraries.defaultFor(OffsetDateTime.class)";
      default -> "Arbitraries.defaultFor(%s.class)".formatted(javaType(fieldVar.getType()));
    };
  }

  private String textArbitrary(List<String> definitions) {
    var result = "Arbitraries.strings()";
    if (definitions != null && definitions.contains("nonempty")) {
      result += ".ofMinLength(1)";
    }
    return result + ".map(Text::new)";
  }
}
