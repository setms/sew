package org.setms.sew.core.inbound.tool;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.setms.sew.core.domain.model.format.Strings.initLower;
import static org.setms.sew.core.domain.model.tool.Level.ERROR;
import static org.setms.sew.core.domain.model.tool.Level.WARN;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.stakeholders.Owner;
import org.setms.sew.core.domain.model.sdlc.stakeholders.Stakeholder;
import org.setms.sew.core.domain.model.sdlc.stakeholders.User;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.Location;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Suggestion;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.format.sal.SalFormat;

public class StakeholdersTool extends Tool {

  private static final String STAKEHOLDERS_PATH = "src/main/stakeholders";
  private static final String SUGGESTION_CREATE_OWNER = "stakeholders.createOwner";

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>("users", new Glob(STAKEHOLDERS_PATH, "**/*.user"), new SalFormat(), User.class),
        new Input<>(
            "owners", new Glob(STAKEHOLDERS_PATH, "**/*.owner"), new SalFormat(), Owner.class),
        new Input<>(
            "useCases",
            new Glob("src/main/requirements", "**/*.useCase"),
            new SalFormat(),
            UseCase.class));
  }

  @Override
  public List<Output> getOutputs() {
    return emptyList();
  }

  @Override
  protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var owners = inputs.get(Owner.class);
    validateOwner(owners, diagnostics);
    var users = inputs.get(User.class);
    var useCases = inputs.get(UseCase.class);
    validateUseCaseUsers(useCases, users, owners, diagnostics);
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

  private void validateUseCaseUsers(
      List<UseCase> useCases,
      List<User> users,
      List<Owner> owners,
      Collection<Diagnostic> diagnostics) {
    useCases.forEach(useCase -> validateUseCaseUsers(useCase, users, owners, diagnostics));
  }

  private void validateUseCaseUsers(
      UseCase useCase, List<User> users, List<Owner> owners, Collection<Diagnostic> diagnostics) {
    useCase
        .getScenarios()
        .forEach(
            scenario ->
                scenario
                    .getSteps()
                    .forEach(
                        step ->
                            validateStepUsers(
                                new Location(useCase)
                                    .plus(scenario)
                                    .plus("steps", scenario.getSteps(), step),
                                step,
                                users,
                                owners,
                                diagnostics)));
  }

  private void validateStepUsers(
      Location location,
      Pointer step,
      List<User> users,
      List<Owner> owners,
      Collection<Diagnostic> diagnostics) {
    if ("user".equals(step.getType())) {
      var name = step.getId();
      var user = find(name, users);
      if (user.isEmpty()) {
        var owner = find(name, owners);
        if (owner.isPresent()) {
          diagnostics.add(
              new Diagnostic(
                  ERROR,
                  "Only users can appear in use case scenarios, found owner " + name,
                  location));
        } else {
          diagnostics.add(new Diagnostic(ERROR, "Unknown user " + name, location));
        }
      }
    }
  }

  private <T extends Stakeholder> Optional<T> find(String name, List<T> candidates) {
    return candidates.stream().filter(candidate -> name.equals(candidate.getName())).findFirst();
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
        inputs.get(User.class).stream().map(NamedObject::getPackage).collect(Collectors.toSet());
    var stakeholdersSink = toBase(sink).select(STAKEHOLDERS_PATH);
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
