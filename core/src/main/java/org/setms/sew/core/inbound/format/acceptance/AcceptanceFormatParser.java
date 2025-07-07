package org.setms.sew.core.inbound.format.acceptance;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.setms.sew.core.domain.model.format.Strings.initUpper;
import static org.setms.sew.core.domain.model.format.Strings.stripQuotesFrom;

import java.io.IOException;
import java.io.InputStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.setms.sew.core.domain.model.format.DataEnum;
import org.setms.sew.core.domain.model.format.DataItem;
import org.setms.sew.core.domain.model.format.DataList;
import org.setms.sew.core.domain.model.format.DataString;
import org.setms.sew.core.domain.model.format.NestedObject;
import org.setms.sew.core.domain.model.format.Parser;
import org.setms.sew.core.domain.model.format.Reference;
import org.setms.sew.core.domain.model.format.RootObject;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.lang.acceptance.AcceptanceLexer;
import org.setms.sew.lang.acceptance.AcceptanceParser;

class AcceptanceFormatParser implements Parser {

  @Override
  public RootObject parse(InputStream input) throws IOException {
    return parseUsing(parserFrom(input));
  }

  private AcceptanceParser parserFrom(InputStream input) throws IOException {
    return new AcceptanceParser(
        new CommonTokenStream((new AcceptanceLexer(CharStreams.fromStream(input, UTF_8)))));
  }

  private RootObject parseUsing(AcceptanceParser parser) {
    var test = parser.test();
    var result = parseSut(test);
    if (result != null) {
      var variables = parseVariables(test.variables());
      result.set("variables", variables);
      result.set("scenarios", parseScenarios(test.scenarios(), variables));
    }
    return result;
  }

  private RootObject parseSut(AcceptanceParser.TestContext test) {
    if (test.sut() == null || test.sut().sut_row() == null) {
      return null;
    }
    var sut = test.sut().sut_row();
    if (sut.TYPE() == null || sut.qualifiedName() == null) {
      return null;
    }
    var type = sut.TYPE().getText();
    var fqn = sut.qualifiedName().getText();
    var nameParts = fqn.split("\\.");
    var name = nameParts[nameParts.length - 1];
    return new RootObject(nameParts[0], "acceptanceTest", name + initUpper(type))
        .set("sut", new Reference(type, name));
  }

  private DataList parseVariables(AcceptanceParser.VariablesContext variables) {
    var result = new DataList();
    if (variables != null && variables.variables_row() != null) {
      variables.variables_row().forEach(row -> addVariable(row, result));
    }
    return result;
  }

  private void addVariable(AcceptanceParser.Variables_rowContext row, DataList variables) {
    if (row.item() == null || row.type() == null) {
      return;
    }
    var type = parseVariableType(row.type());
    if (type == null) {
      return;
    }
    var variable = new NestedObject(row.item().getText()).set("type", type);
    if (row.definition() != null) {
      var definitions = parseVariableDefinitions(row.definition());
      if (definitions.hasItems()) {
        variable.set("definitions", definitions);
      }
    }
    variables.add(variable);
  }

  private DataItem parseVariableType(AcceptanceParser.TypeContext type) {
    var identifier = type.IDENTIFIER();
    if (identifier != null) {
      return new DataEnum(identifier.getText());
    }
    var typedReference = type.typedReference();
    if (typedReference != null
        && typedReference.TYPE() != null
        && typedReference.OBJECT_NAME() != null) {
      return new Reference(typedReference.TYPE().getText(), typedReference.OBJECT_NAME().getText());
    }
    return null;
  }

  private DataList parseVariableDefinitions(AcceptanceParser.DefinitionContext definition) {
    var result = new DataList();
    if (definition.constraints() == null && definition.fields() == null) {
      return result;
    }
    var constraints = definition.constraints();
    if (constraints != null) {
      constraints.constraint().stream()
          .map(AcceptanceParser.ConstraintContext::getText)
          .map(DataEnum::new)
          .forEach(result::add);
      return result;
    }
    definition
        .fields()
        .field()
        .forEach(
            field -> {
              if (field.OBJECT_NAME() == null) {
                return;
              }
              var name = field.OBJECT_NAME().getText();
              var assignment = new NestedObject(name).set("fieldName", new DataString(name));
              var string = field.STRING();
              if (string != null) {
                assignment.set("value", new DataString(string.getText()));
              } else {
                var value = field.IDENTIFIER();
                if (value == null) {
                  return;
                }
                assignment.set("value", new Reference("variable", value.getText()));
              }
              result.add(assignment);
            });
    return result;
  }

  private DataList parseScenarios(AcceptanceParser.ScenariosContext scenarios, DataList variables) {
    var result = new DataList();
    scenarios.scenario_row().forEach(row -> addScenario(row, variables, result));
    return result;
  }

  private void addScenario(
      AcceptanceParser.Scenario_rowContext row, DataList variables, DataList scenarios) {
    if (row.STRING() == null || row.item() == null || row.item().size() < 2) {
      return;
    }
    var name = stripQuotesFrom(row.STRING().getText());
    var scenario = new NestedObject(name);
    var numCells = row.item().size();
    var initialized = false;
    for (var index = 0; index < numCells; index++) {
      var variableName = row.item(index).getText();
      var reference = new Reference("variable", variableName);
      var property =
          switch (variableTypeOf(variableName, variables)) {
            case "entity" -> initialized ? "state" : "init";
            case "command" -> "command";
            case "event" -> "emitted";
            default -> null;
          };
      if (property == null) {
        return;
      }
      scenario.set(property, reference);
      initialized = true;
    }
    scenarios.add(scenario);
  }

  private String variableTypeOf(String name, DataList variables) {
    return variables
        .map(NestedObject.class::cast)
        .filter(object -> object.getName().equals(name))
        .findFirst()
        .map(object -> object.property("type"))
        .filter((Reference.class::isInstance))
        .map(Reference.class::cast)
        .map(Reference::getType)
        .orElse(null);
  }

  @Override
  public NamedObject createObject(
      NestedObject source, String name, Object parent, boolean validate) {
    var type = name;
    if ("variables".equals(name)) {
      type = source.property("type") instanceof DataEnum ? "fieldVariable" : "elementVariable";
    } else if ("definitions".equals(name)) {
      type = "fieldAssignment";
    }
    return Parser.super.createObject(source, type, parent, validate);
  }
}
