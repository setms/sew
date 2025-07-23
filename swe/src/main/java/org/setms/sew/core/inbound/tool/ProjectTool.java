package org.setms.sew.core.inbound.tool;

import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.sew.core.inbound.tool.Inputs.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Output;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.OutputSink;
import org.setms.sew.core.domain.model.sdlc.stakeholders.Owner;
import org.setms.sew.core.domain.model.sdlc.stakeholders.User;
import org.setms.sew.core.inbound.format.sal.SalFormat;

public class ProjectTool extends BaseTool {

  private static final String SUGGESTION_CREATE_OWNER = "stakeholders.createOwner";

  @Override
  public List<Input<?>> getInputs() {
    return List.of(owners(), users());
  }

  @Override
  public Optional<Output> getOutputs() {
    return Optional.empty();
  }

  @Override
  protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var owners = inputs.get(Owner.class);
    validateOwner(owners, diagnostics);
  }

  public void validateOwner(List<Owner> owners, Collection<Diagnostic> diagnostics) {
    if (owners.isEmpty()) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing owner",
              null,
              List.of(new Suggestion(SUGGESTION_CREATE_OWNER, "Create owner"))));
    } else if (owners.size() > 1) {
      diagnostics.add(
          new Diagnostic(
              ERROR,
              "There can be only one owner, but found "
                  + owners.stream().map(Owner::getName).sorted().collect(joining(", "))));
    }
  }

  @Override
  public void apply(
      String suggestionCode,
      ResolvedInputs inputs,
      Location location,
      OutputSink sink,
      Collection<Diagnostic> diagnostics) {
    if (SUGGESTION_CREATE_OWNER.equals(suggestionCode)) {
      createOwner(sink, inputs, diagnostics);
    } else {
      super.apply(suggestionCode, inputs, location, sink, diagnostics);
    }
  }

  private void createOwner(
      OutputSink sink, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var packages =
        inputs.get(User.class).stream().map(Artifact::getPackage).collect(Collectors.toSet());
    var stakeholdersSink = toBase(sink).select(Inputs.PATH_STAKEHOLDERS);
    try {
      var scope =
          packages.size() == 1 ? packages.iterator().next() : scopeOf(sink, stakeholdersSink);
      var owner = new Owner(new FullyQualifiedName(scope + ".Some")).setDisplay("<Some role>");
      var ownerSink = stakeholdersSink.select(owner.getName() + ".owner");
      try (var output = ownerSink.open()) {
        new SalFormat().newBuilder().build(owner, output);
      }
      diagnostics.add(sinkCreated(ownerSink));
    } catch (Exception e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
  }

  private String scopeOf(OutputSink sink, OutputSink stakeholders) {
    var containers = stakeholders.containers();
    var uri = containers.isEmpty() ? sink.toUri() : containers.getFirst().toUri();
    var result = uri.getPath();
    if (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    result = result.substring(1 + result.lastIndexOf('/'));
    var index = result.lastIndexOf('.');
    if (index > 0) {
      result = result.substring(0, index);
    }
    return initLower(result);
  }
}
