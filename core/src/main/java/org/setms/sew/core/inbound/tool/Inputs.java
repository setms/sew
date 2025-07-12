package org.setms.sew.core.inbound.tool;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
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
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.inbound.format.acceptance.AcceptanceFormat;

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
    return new Input<>(PATH_DESIGN, Aggregate.class);
  }

  static Input<ClockEvent> clockEvents() {
    return new Input<>(PATH_DESIGN, ClockEvent.class);
  }

  static Input<Command> commands() {
    return new Input<>(PATH_DESIGN, Command.class);
  }

  static Input<Domain> domains() {
    return new Input<>(PATH_ANALYSIS, Domain.class);
  }

  static Input<DomainStory> domainStories() {
    return new Input<>(PATH_REQUIREMENTS, DomainStory.class);
  }

  static Input<Entity> entities() {
    return new Input<>(PATH_DESIGN, Entity.class);
  }

  static Input<ExternalSystem> externalSystems() {
    return new Input<>(PATH_DESIGN, ExternalSystem.class);
  }

  static Input<Event> events() {
    return new Input<>(PATH_DESIGN, Event.class);
  }

  static Input<Modules> modules() {
    return new Input<>(PATH_ARCHITECTURE, Modules.class);
  }

  static Input<Owner> owners() {
    return new Input<>(PATH_STAKEHOLDERS, Owner.class);
  }

  static Input<Policy> policies() {
    return new Input<>(PATH_DESIGN, Policy.class);
  }

  static Input<ReadModel> readModels() {
    return new Input<>(PATH_DESIGN, ReadModel.class);
  }

  static Input<Term> terms() {
    return new Input<>("src/main/glossary", Term.class);
  }

  static Input<UseCase> useCases() {
    return new Input<>(PATH_REQUIREMENTS, UseCase.class);
  }

  static Input<User> users() {
    return new Input<>(PATH_STAKEHOLDERS, User.class);
  }
}
