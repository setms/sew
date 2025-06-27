package org.setms.sew.core.inbound.format.acceptance;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import org.setms.sew.core.domain.model.format.Builder;
import org.setms.sew.core.domain.model.format.DataEnum;
import org.setms.sew.core.domain.model.format.DataItem;
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
    Optional.ofNullable(root)
        .map(object -> object.property("sut", Reference.class))
        .ifPresentOrElse(
            sut -> table.addRow(sut.getType(), "%s.%s".formatted(root.getScope(), sut.getId())),
            () -> table.addRow("???", "???"));
    table.printTo(writer);
  }

  private void buildVariables(DataList variables, PrintWriter writer) {
    var table = new Table("variable", "type", "definition");
    if (variables != null && variables.hasItems()) {
      variables
          .map(NestedObject.class::cast)
          .forEach(
              object -> table.addRow(object.getName(), buildType(object), buildDefinition(object)));
    }
    table.printTo(writer);
  }

  private String buildType(NestedObject object) {
    var type = object.property("type");
    return switch (type) {
      case Reference reference -> "%s(%s)".formatted(reference.getType(), reference.getId());
      case DataEnum value -> value.getName();
      default ->
          throw new UnsupportedOperationException(
              "Unsupported type: " + type.getClass().getSimpleName());
    };
  }

  private String buildDefinition(NestedObject object) {
    return buildDefinitionPart(object.property("definitions"));
  }

  private String buildDefinitionPart(DataItem definition) {
    return switch (definition) {
      case null -> "";
      case DataString string -> string.getValue();
      case DataEnum value -> value.getName();
      case Reference reference -> reference.getId();
      case DataList list -> buildDefinitions(list);
      case NestedObject object -> buildNestedDefinition(object);
      default ->
          throw new UnsupportedOperationException(
              "Unsupported definition: " + definition.getClass().getSimpleName());
    };
  }

  private String buildDefinitions(DataList list) {
    return list.map(this::buildDefinitionPart).collect(joining(", "));
  }

  private String buildNestedDefinition(NestedObject nested) {
    return "%s=%s"
        .formatted(
            buildDefinitionPart(nested.property("fieldName")),
            buildDefinitionPart(nested.property("value")));
  }

  private void buildScenarios(DataList scenarios, PrintWriter writer) {
    var table = new Table("scenario", "init", "command", "state", "emitted");
    if (scenarios != null && scenarios.hasItems()) {
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
    }
    table.printTo(writer);
  }
}
