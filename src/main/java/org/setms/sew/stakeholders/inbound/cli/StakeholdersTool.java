package org.setms.sew.stakeholders.inbound.cli;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.setms.sew.tool.Level.ERROR;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.setms.sew.format.sew.SewFormat;
import org.setms.sew.schema.FullyQualifiedName;
import org.setms.sew.schema.Pointer;
import org.setms.sew.tool.Diagnostic;
import org.setms.sew.tool.Glob;
import org.setms.sew.tool.Input;
import org.setms.sew.tool.Output;
import org.setms.sew.tool.ResolvedInputs;
import org.setms.sew.tool.Suggestion;
import org.setms.sew.tool.Tool;

public class StakeholdersTool extends Tool {

  private static final Pattern PERSON = Pattern.compile("Person\\((?<name>[a-zA-Z0-9_()]+)\\)");
  private static final String STAKEHOLDERS_PATH = "src/main/stakeholders";
  private static final String SUGGESTION_CREATE_OWNER = "stakeholders.createOwner";

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>("users", new Glob(STAKEHOLDERS_PATH, "**/*.user"), new SewFormat(), User.class),
        new Input<>(
            "owners", new Glob(STAKEHOLDERS_PATH, "**/*.owner"), new SewFormat(), Owner.class),
        new Input<>(
            "useCases",
            new Glob("src/main/requirements", "**/*.useCase"),
            new SewFormat(),
            UseCase.class));
  }

  @Override
  public List<Output> getOutputs() {
    return emptyList();
  }

  @Override
  protected void validate(File dir, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var owners = inputs.get("owners", Owner.class);
    validateOwner(owners, diagnostics);
    var users = inputs.get("users", User.class);
    var useCases = inputs.get("useCases", UseCase.class);
    validateUseCaseUsers(useCases, users, owners, diagnostics);
  }

  public void validateOwner(List<Owner> owners, Collection<Diagnostic> diagnostics) {
    if (owners.isEmpty()) {
      diagnostics.add(
          new Diagnostic(
              ERROR,
              "Missing owner",
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
    useCase.getScenarios().stream()
        .map(UseCase.Scenario::getSteps)
        .flatMap(Collection::stream)
        .forEach(step -> validateStepUsers(step, users, owners, diagnostics));
  }

  private void validateStepUsers(
      Pointer step, List<User> users, List<Owner> owners, Collection<Diagnostic> diagnostics) {
    var matcher = PERSON.matcher(step.getId());
    if (matcher.matches()) {
      var name = matcher.group("name");
      var user = find(name, users);
      if (user.isEmpty()) {
        var owner = find(name, owners);
        if (owner.isPresent()) {
          diagnostics.add(
              new Diagnostic(
                  ERROR, "Only users can appear in use case scenarios, found owner " + name));
        } else {
          diagnostics.add(new Diagnostic(ERROR, "Unknown user " + name));
        }
      }
    }
  }

  private <T extends Stakeholder> Optional<T> find(String name, List<T> candidates) {
    return candidates.stream().filter(candidate -> name.equals(candidate.getName())).findFirst();
  }

  @Override
  public void apply(
      String suggestionCode, File dir, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (SUGGESTION_CREATE_OWNER.equals(suggestionCode)) {
      createOwner(dir, inputs, diagnostics);
    } else {
      super.apply(suggestionCode, dir, inputs, diagnostics);
    }
  }

  private void createOwner(File dir, ResolvedInputs ignored, Collection<Diagnostic> diagnostics) {
    var stakeholdersDir = new File(dir, STAKEHOLDERS_PATH);
    try {
      var scope = scopeOf(dir, stakeholdersDir);
      var owner = new Owner(new FullyQualifiedName(scope + ".Some")).setDisplay("<Some role>");
      var ownerFile = new File(stakeholdersDir, owner.getName() + ".owner");
      new SewFormat().newBuilder().build(owner, ownerFile);
      diagnostics.add(fileCreated(ownerFile));
    } catch (Exception e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
    }
  }

  private String scopeOf(File baseDir, File stakeholdersDir) {
    var children = stakeholdersDir.listFiles();
    if (children != null) {
      var subdirs =
          Arrays.stream(children)
              .filter(File::isDirectory)
              .filter(d -> !d.getName().startsWith("."))
              .toList();
      if (subdirs.size() == 1) {
        return subdirs.getFirst().getName();
      }
    }
    try {
      return baseDir.getCanonicalFile().getName();
    } catch (IOException e) {
      throw new IllegalStateException(e.getMessage());
    }
  }
}
