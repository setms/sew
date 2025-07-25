package org.setms.sew.core.inbound.tool;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.tool.Input;
import org.setms.sew.core.domain.model.sdlc.acceptance.AcceptanceTest;
import org.setms.sew.core.domain.model.sdlc.architecture.Modules;
import org.setms.sew.core.domain.model.sdlc.ddd.Domain;
import org.setms.sew.core.domain.model.sdlc.ddd.Term;
import org.setms.sew.core.domain.model.sdlc.design.Entity;
import org.setms.sew.core.domain.model.sdlc.domainstory.DomainStory;
import org.setms.sew.core.domain.model.sdlc.eventstorming.*;
import org.setms.sew.core.domain.model.sdlc.stakeholders.Owner;
import org.setms.sew.core.domain.model.sdlc.stakeholders.User;
import org.setms.sew.core.domain.model.sdlc.usecase.UseCase;
import org.setms.sew.core.inbound.format.acceptance.AcceptanceFormat;
import org.setms.sew.core.inbound.format.sal.SalFormat;

@NoArgsConstructor(access = PRIVATE)
class Inputs {

  static final String PATH_DESIGN = "src/main/design";
  static final String PATH_REQUIREMENTS = "src/main/requirements";
  static final String PATH_STAKEHOLDERS = "src/main/stakeholders";
  static final String PATH_ACCEPTANCE_TESTS = "src/test/acceptance";
  static final String PATH_ANALYSIS = "src/main/analysis";
  static final String PATH_ARCHITECTURE = "src/main/architecture";

  static Input<AcceptanceTest> acceptanceTests() {
    return new Input<>(
        PATH_ACCEPTANCE_TESTS, new AcceptanceFormat(), AcceptanceTest.class, "acceptance");
  }

  static Input<Aggregate> aggregates() {
    return newInput(PATH_DESIGN, Aggregate.class);
  }

  private static <T extends Artifact> Input<T> newInput(String path, Class<T> type) {
    return new Input<>(path, new SalFormat(), type);
  }

  static Input<ClockEvent> clockEvents() {
    return newInput(PATH_DESIGN, ClockEvent.class);
  }

  static Input<Command> commands() {
    return newInput(PATH_DESIGN, Command.class);
  }

  static Input<Domain> domains() {
    return newInput(PATH_ANALYSIS, Domain.class);
  }

  static Input<DomainStory> domainStories() {
    return newInput(PATH_REQUIREMENTS, DomainStory.class);
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

  static Input<Owner> owners() {
    return newInput(PATH_STAKEHOLDERS, Owner.class);
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
    return newInput(PATH_REQUIREMENTS, UseCase.class);
  }

  static Input<User> users() {
    return newInput(PATH_STAKEHOLDERS, User.class);
  }
}
