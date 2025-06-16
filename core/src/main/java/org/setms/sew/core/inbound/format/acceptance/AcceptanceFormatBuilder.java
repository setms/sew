package org.setms.sew.core.inbound.format.acceptance;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicReference;
import org.setms.sew.core.domain.model.format.Builder;
import org.setms.sew.core.domain.model.format.DataEnum;
import org.setms.sew.core.domain.model.format.DataList;
import org.setms.sew.core.domain.model.format.DataString;
import org.setms.sew.core.domain.model.format.NestedObject;
import org.setms.sew.core.domain.model.format.Reference;
import org.setms.sew.core.domain.model.format.RootObject;

class AcceptanceFormatBuilder implements Builder {

  @Override
  public void build(RootObject root, PrintWriter writer) throws IOException {
    buildSut(root, writer);
    writer.println();
    buildVariables(root.property("variables", DataList.class), writer);
    writer.println();
    buildScenarios(root.property("scenarios", DataList.class), writer);
  }

  public void buildSut(RootObject root, PrintWriter writer) {
    var table = new Table("type", "name");
    var sut = root.property("sut", Reference.class);
    table.addRow(sut.getType(), "%s.%s".formatted(root.getScope(), sut.getId()));
    table.printTo(writer);
  }

  private void buildVariables(DataList variables, PrintWriter writer) {
    var table = new Table("variable", "type", "definition");
    variables
        .map(NestedObject.class::cast)
        .forEach(
            object -> table.addRow(object.getName(), buildType(object), buildDefinition(object)));
    table.printTo(writer);
  }

  private String buildType(NestedObject object) {
    var type = object.property("type");
    return switch (type) {
      case Reference reference -> "%s(%s)".formatted(reference.getType(), reference.getId());
      case DataString string -> string.getValue();
      case DataEnum value -> value.getName();
      default ->
          throw new UnsupportedOperationException(
              "Unsupported type: " + type.getClass().getSimpleName());
    };
  }

  private String buildDefinition(NestedObject object) {
    var definition = object.property("definition");
    return switch (definition) {
      case null -> "";
      case DataString string -> string.getValue();
      case DataEnum value -> value.getName();
      case NestedObject nested -> buildNestedDefinition(nested);
      default ->
          throw new UnsupportedOperationException(
              "Unsupported definition: " + definition.getClass().getSimpleName());
    };
  }

  private String buildNestedDefinition(NestedObject nested) {
    var result = new StringBuilder();
    var prefix = new AtomicReference<>("");
    nested.properties(
        (key, value) ->
            result.append(prefix.getAndSet(", ")).append(key).append('=').append(value));
    return result.toString();
  }

  private void buildScenarios(DataList scenarios, PrintWriter writer) {
    var table = new Table("scenario", "init", "command", "state", "emitted");
    scenarios
        .map(NestedObject.class::cast)
        .forEach(
            object -> {
              var name = '"' + object.getName() + '"';
              var init = "";
              var command = object.property("command", Reference.class).getId();
              var state = "";
              var emitted = object.property("emitted", Reference.class).getId();
              table.addRow(name, init, command, state, emitted);
            });
    table.printTo(writer);
  }
}
