package org.setms.swe.inbound.tool;

import static java.util.function.Predicate.not;
import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.format.Strings.toFriendlyName;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.diagram.Arrow;
import org.setms.km.domain.model.diagram.BaseDiagramTool;
import org.setms.km.domain.model.diagram.Box;
import org.setms.km.domain.model.diagram.Diagram;
import org.setms.km.domain.model.diagram.IconBox;
import org.setms.km.domain.model.tool.*;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.domainstory.DomainStory;
import org.setms.swe.domain.model.sdlc.domainstory.Sentence;
import org.setms.swe.domain.model.sdlc.usecase.Scenario;
import org.setms.swe.domain.model.sdlc.usecase.UseCase;
import org.setms.swe.domain.services.DomainStoryToUseCase;

public class DomainStoryTool extends BaseDiagramTool<DomainStory> {

  private static final String CREATE_USE_CASE_SCENARIO = "usecase.scenario.create";

  @Override
  public Input<DomainStory> getMainInput() {
    return domainStories();
  }

  @Override
  public Set<Input<?>> additionalInputs() {
    return Set.of(useCases());
  }

  @Override
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var domainStories = inputs.get(DomainStory.class);
    domainStories.forEach(
        domainStory -> build(domainStory, resource.select(domainStory.getName()), diagnostics));
  }

  private void build(
      DomainStory domainStory, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    buildHtml(
        domainStory,
        domainStory.getDescription(),
        toDiagram(domainStory.getSentences()),
        resource,
        diagnostics);
  }

  private Diagram toDiagram(List<Sentence> sentences) {
    var result = new Diagram();
    for (var i = 0; i < sentences.size(); i++) {
      addSentence(i, sentences.get(i), result);
    }
    return result;
  }

  private void addSentence(int index, Sentence sentence, Diagram diagram) {
    var firstActivity = new AtomicBoolean(true);
    var previousBox = new AtomicReference<Box>();
    var activity = new AtomicReference<String>();
    sentence
        .getParts()
        .forEach(
            part -> {
              switch (part.getType()) {
                case "person" ->
                    addBox(part, "material/person", diagram, "actor", previousBox, activity);
                case "people" ->
                    addBox(part, "material/group", diagram, "actor", previousBox, activity);
                case "computerSystem" ->
                    addBox(part, "material/computer", diagram, "actor", previousBox, activity);
                case "activity" ->
                    activity.set(
                        "%s%s"
                            .formatted(
                                firstActivity.getAndSet(false) ? "%c%n".formatted('①' + index) : "",
                                initLower(toFriendlyName(part.getId()))));
                case "workObject" ->
                    addBox(
                        part,
                        Optional.ofNullable(part.getAttributes().get("icon"))
                            .map(List::getFirst)
                            .map(p -> "%s/%s".formatted(p.getType(), initLower(p.getId())))
                            .orElse("material/folder"),
                        diagram,
                        part == sentence.getParts().getLast() ? "workObject" : null,
                        previousBox,
                        activity);
                default ->
                    throw new UnsupportedOperationException(
                        "Can't add to diagram: " + part.getType());
              }
            });
  }

  private void addBox(
      Link part,
      String iconPath,
      Diagram diagram,
      String reuseType,
      AtomicReference<Box> previousBox,
      AtomicReference<String> activity) {
    var box =
        diagram.add(
            new IconBox(
                toFriendlyName(part.getId()),
                "domainStory/" + iconPath,
                "domainStory/material/questionMark"),
            reuseType);
    Optional.ofNullable(previousBox.getAndSet(box))
        .map(from -> new Arrow(from, box, activity.get()))
        .ifPresent(diagram::add);
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var useCases = inputs.get(UseCase.class);
    inputs
        .get(DomainStory.class)
        .forEach(domainStory -> validate(domainStory, useCases, diagnostics));
  }

  private void validate(
      DomainStory domainStory, Collection<UseCase> useCases, Collection<Diagnostic> diagnostics) {
    if (useCases.stream()
        .flatMap(UseCase::scenarios)
        .map(Scenario::getElaborates)
        .filter(Objects::nonNull)
        .noneMatch(elaborates -> elaborates.pointsTo(domainStory))) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Not elaborated in use case scenario",
              domainStory.toLocation(),
              elaborationSuggestions(useCases)));
    }
  }

  private List<Suggestion> elaborationSuggestions(Collection<UseCase> useCases) {
    return Stream.concat(
            Stream.of(new Suggestion(CREATE_USE_CASE_SCENARIO, "Elaborate in new use case")),
            useCases.stream()
                .map(Artifact::getFullyQualifiedName)
                .map(
                    name ->
                        new Suggestion(
                            "%s.%s".formatted(CREATE_USE_CASE_SCENARIO, name),
                            "Elaborate in use case %s".formatted(name))))
        .toList();
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> domainStoryResource,
      DomainStory domainStory,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs) {
    if (suggestionCode.startsWith(CREATE_USE_CASE_SCENARIO)) {
      return elaborateInUseCase(
          domainStoryResource, domainStory, extractUseCaseFrom(suggestionCode, inputs));
    }
    return unknown(suggestionCode);
  }

  private Optional<UseCase> extractUseCaseFrom(String suggestionCode, ResolvedInputs inputs) {
    return extractUseCaseNameFrom(suggestionCode).flatMap(name -> inputs.find(UseCase.class, name));
  }

  private Optional<FullyQualifiedName> extractUseCaseNameFrom(String suggestionCode) {
    return Optional.of(suggestionCode)
        .map(code -> code.substring(CREATE_USE_CASE_SCENARIO.length()))
        .filter(not(String::isEmpty))
        .map(code -> code.substring(1))
        .map(FullyQualifiedName::new);
  }

  private AppliedSuggestion elaborateInUseCase(
      Resource<?> domainStoryResource, DomainStory domainStory, Optional<UseCase> target) {
    try {
      var converter = new DomainStoryToUseCase();
      var useCase =
          target
              .map(uc -> converter.addScenarioFrom(domainStory, uc))
              .orElseGet(() -> converter.createUseCaseFrom(domainStory));
      var useCaseResource = resourceFor(useCase, domainStory, domainStoryResource);
      try (var output = useCaseResource.writeTo()) {
        builderFor(useCase).build(useCase, output);
      }
      return created(useCaseResource);
    } catch (Exception e) {
      return failedWith(e);
    }
  }
}
