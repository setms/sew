package org.setms.swe.inbound.tool;

import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.Tool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.stakeholders.Owner;
import org.setms.swe.domain.model.sdlc.stakeholders.User;

public class ProjectTool extends Tool<Owner> {

  private static final String SUGGESTION_CREATE_OWNER = "stakeholders.createOwner";

  @Override
  public Input<Owner> getMainInput() {
    return owners();
  }

  @Override
  public Set<Input<? extends Artifact>> additionalInputs() {
    return Set.of(users());
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
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
  protected AppliedSuggestion doApply(
      Resource<?> ownerResource,
      Owner owner,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs)
      throws IOException {
    if (SUGGESTION_CREATE_OWNER.equals(suggestionCode)) {
      return createOwner(ownerResource, inputs);
    }
    return unknown(suggestionCode);
  }

  private AppliedSuggestion createOwner(Resource<?> resource, ResolvedInputs inputs)
      throws IOException {
    var packages =
        inputs.get(User.class).stream().map(Artifact::getPackage).collect(Collectors.toSet());
    var stakeholdersResource = resource.select(Inputs.PATH_STAKEHOLDERS);
    var scope =
        packages.size() == 1 ? packages.iterator().next() : scopeOf(resource, stakeholdersResource);
    var owner = new Owner(new FullyQualifiedName(scope + ".Some")).setDisplay("<Some role>");
    var ownerResource = stakeholdersResource.select(owner.getName() + ".owner");
    try (var output = ownerResource.writeTo()) {
      builderFor(owner).build(owner, output);
    }
    return created(ownerResource);
  }

  private String scopeOf(Resource<?> resource, Resource<?> stakeholders) {
    var containers = stakeholders.children();
    var uri = containers.isEmpty() ? resource.toUri() : containers.getFirst().toUri();
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
