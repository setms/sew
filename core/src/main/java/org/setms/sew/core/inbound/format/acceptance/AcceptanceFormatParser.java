package org.setms.sew.core.inbound.format.acceptance;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.setms.sew.core.domain.model.format.Strings.stripQuotesFrom;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
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
    var result = parseRootObject(test);
    var variables = parseVariables(test.variables());
    result.set("variables", variables);
    result.set("scenarios", parseScenarios(test.scenarios(), variables));
    return result;
  }

  private RootObject parseRootObject(AcceptanceParser.TestContext test) {
    if (test.sut() == null || test.sut().table() == null || test.sut().table().row() == null) {
      return null;
    }
    var rows = test.sut().table().row();
    if (rows.size() != 1) {
      throw new IllegalArgumentException("Must have one row to define System Under Test");
    }
    var sut = rows.getFirst();
    var type = sut.cell(0).getText();
    var fqn = sut.cell(1).getText();
    var nameParts = fqn.split("\\.");
    var name = nameParts[nameParts.length - 1];
    return new RootObject(nameParts[0], "acceptanceTest", type + "." + name)
        .set("sut", new Reference(type, name));
  }

  private DataList parseVariables(AcceptanceParser.VariablesContext variables) {
    var result = new DataList();
    variables.table().row().forEach(row -> addVariable(row, result));
    return result;
  }

  private void addVariable(AcceptanceParser.RowContext row, DataList variables) {
    var numCells = row.cell().size();
    if (numCells < 2 || numCells > 3) {
      return;
    }
    var type = parseVariableType(row.cell(1));
    if (type == null) {
      return;
    }
    var variable = new NestedObject(row.cell(0).getText()).set("type", type);
    if (numCells == 3) {
      parseVariableDefinition(row.cell(2))
          .ifPresent(definition -> variable.set("definition", definition));
    }
    variables.add(variable);
  }

  private DataItem parseVariableType(AcceptanceParser.CellContext type) {
    var objectName = type.OBJECT_NAME();
    if (objectName != null) {
      return new DataEnum(objectName.getText());
    }
    var typedReference = type.typedReference();
    if (typedReference != null) {
      return new Reference(typedReference.TYPE().getText(), typedReference.OBJECT_NAME().getText());
    }
    return null;
  }

  private Optional<DataItem> parseVariableDefinition(AcceptanceParser.CellContext cell) {
    var objectName = cell.OBJECT_NAME();
    if (objectName != null) {
      return Optional.of(new DataEnum(objectName.getText()));
    }
    var fields = cell.fields();
    if (fields != null) {
      var result = new NestedObject("definition");
      fields
          .field()
          .forEach(
              field -> {
                if (field.OBJECT_NAME() == null) {
                  return;
                }
                var name = field.OBJECT_NAME().getText();
                var string = field.STRING();
                if (string != null) {
                  result.set(name, new DataString(string.getText()));
                  return;
                }
                var identifier = field.IDENTIFIER();
                if (identifier != null) {
                  result.set(name, new Reference("variable", identifier.getText()));
                }
              });
      return Optional.of(result);
    }
    return Optional.empty();
  }

  private DataList parseScenarios(AcceptanceParser.ScenariosContext scenarios, DataList variables) {
    var result = new DataList();
    scenarios.table().row().forEach(row -> addScenario(row, variables, result));
    return result;
  }

  private void addScenario(
      AcceptanceParser.RowContext row, DataList variables, DataList scenarios) {
    var numCells = row.cell().size();
    if (numCells < 3 || numCells > 5) {
      return;
    }
    var name = row.cell(0);
    if (name.STRING() == null) {
      return;
    }
    var scenario = new NestedObject(stripQuotesFrom(name.STRING().getText()));
    var initialized = false;
    for (var index = 1; index < numCells; index++) {
      var variableName = row.cell(index).getText();
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
}
