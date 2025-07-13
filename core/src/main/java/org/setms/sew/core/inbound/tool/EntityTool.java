package org.setms.sew.core.inbound.tool;

import static org.setms.sew.core.domain.model.validation.Level.ERROR;
import static org.setms.sew.core.inbound.tool.Inputs.entities;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.setms.sew.core.domain.model.sdlc.design.Entity;
import org.setms.sew.core.domain.model.sdlc.design.Field;
import org.setms.sew.core.domain.model.sdlc.design.FieldType;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.domain.model.validation.Diagnostic;
import org.setms.sew.core.domain.model.validation.Location;

public class EntityTool extends Tool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(entities());
  }

  @Override
  public List<Output> getOutputs() {
    return List.of();
  }

  @Override
  protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    inputs.get(Entity.class).forEach(entity -> validate(entity, diagnostics));
  }

  private void validate(Entity entity, Collection<Diagnostic> diagnostics) {
    var location = new Location(entity);
    Stream.ofNullable(entity.getFields())
        .flatMap(Collection::stream)
        .forEach(field -> validate(field, location.plus(field), diagnostics));
  }

  private void validate(Field field, Location location, Collection<Diagnostic> diagnostics) {
    if (field.getType() == FieldType.SELECTION
        && (field.getValues() == null || field.getValues().isEmpty())) {
      diagnostics.add(new Diagnostic(ERROR, "Selection field needs values to select", location));
    }
  }
}
