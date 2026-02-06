package org.setms.swe.inbound.tool;

import static lombok.AccessLevel.PRIVATE;

import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.GlobInput;
import org.setms.km.domain.model.tool.Input;
import org.setms.swe.domain.model.sdlc.acceptancetest.AcceptanceTest;
import org.setms.swe.domain.model.sdlc.architecture.Components;
import org.setms.swe.domain.model.sdlc.architecture.Decision;
import org.setms.swe.domain.model.sdlc.architecture.Modules;
import org.setms.swe.domain.model.sdlc.code.CodeFormat;
import org.setms.swe.domain.model.sdlc.code.ProgrammingLanguageConventions;
import org.setms.swe.domain.model.sdlc.ddd.Domain;
import org.setms.swe.domain.model.sdlc.ddd.Term;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.domainstory.DomainStory;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.ClockEvent;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.eventstorming.ExternalSystem;
import org.setms.swe.domain.model.sdlc.eventstorming.Policy;
import org.setms.swe.domain.model.sdlc.eventstorming.ReadModel;
import org.setms.swe.domain.model.sdlc.stakeholders.User;
import org.setms.swe.domain.model.sdlc.unittest.UnitTest;
import org.setms.swe.domain.model.sdlc.usecase.UseCase;
import org.setms.swe.inbound.format.acceptance.AcceptanceFormat;
import org.setms.swe.inbound.format.sal.SalFormat;

@NoArgsConstructor(access = PRIVATE)
class Inputs {

  static final String PATH_STAKEHOLDERS = "src/main/stakeholders";
  static final String PATH_REQUIREMENTS = "src/main/requirements";
  static final String PATH_USE_CASES = PATH_REQUIREMENTS + "/use-cases";
  static final String PATH_DOMAIN_STORIES = PATH_REQUIREMENTS + "/domain-stories";
  static final String PATH_DESIGN = "src/main/design";
  static final String PATH_ACCEPTANCE_TESTS = "src/test/acceptance";
  static final String PATH_ANALYSIS = "src/main/analysis";
  static final String PATH_ARCHITECTURE = "src/main/architecture";

  static Input<AcceptanceTest> acceptanceTests() {
    return new GlobInput<>(
        PATH_ACCEPTANCE_TESTS, AcceptanceFormat.INSTANCE, AcceptanceTest.class, "acceptance");
  }

  static Input<Aggregate> aggregates() {
    return newInput(PATH_DESIGN, Aggregate.class);
  }

  private static <T extends Artifact> Input<T> newInput(String path, Class<T> type) {
    return new GlobInput<>(path, SalFormat.INSTANCE, type);
  }

  static Input<ClockEvent> clockEvents() {
    return newInput(PATH_DESIGN, ClockEvent.class);
  }

  static Input<Command> commands() {
    return newInput(PATH_DESIGN, Command.class);
  }

  static Input<Components> components() {
    return newInput(PATH_ARCHITECTURE, Components.class);
  }

  static Input<Decision> decisions() {
    return newInput(PATH_ARCHITECTURE, Decision.class);
  }

  static Input<Domain> domains() {
    return newInput(PATH_ANALYSIS, Domain.class);
  }

  static Input<DomainStory> domainStories() {
    return newInput(PATH_DOMAIN_STORIES, DomainStory.class);
  }

  static Input<Entity> entities() {
    return newInput(PATH_DESIGN, Entity.class);
  }

  static Input<ExternalSystem> externalSystems() {
    return newInput(PATH_DESIGN, ExternalSystem.class);
  }

  static Input<Event> events() {
    return newInput(PATH_DESIGN, Event.class);
  }

  static Input<Modules> modules() {
    return newInput(PATH_ARCHITECTURE, Modules.class);
  }

  static Input<Policy> policies() {
    return newInput(PATH_DESIGN, Policy.class);
  }

  static Input<ReadModel> readModels() {
    return newInput(PATH_DESIGN, ReadModel.class);
  }

  static Input<Term> terms() {
    return newInput("src/main/glossary", Term.class);
  }

  static Input<UseCase> useCases() {
    return newInput(PATH_USE_CASES, UseCase.class);
  }

  static Input<User> users() {
    return newInput(PATH_STAKEHOLDERS, User.class);
  }

  public static Set<Input<UnitTest>> unitTests() {
    var result = new HashSet<Input<UnitTest>>();
    for (var conventions :
        ServiceLoader.load(
            ProgrammingLanguageConventions.class,
            ProgrammingLanguageConventions.class.getClassLoader())) {
      result.add(
          new GlobInput<>(
              conventions.unitTestPath(),
              new CodeFormat(conventions),
              UnitTest.class,
              conventions.extension()));
    }
    return result;
  }
}
