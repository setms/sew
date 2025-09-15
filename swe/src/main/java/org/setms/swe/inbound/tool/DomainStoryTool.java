package org.setms.swe.inbound.tool;

import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static org.setms.km.domain.model.diagram.Arrow.newTextPlacements;
import static org.setms.km.domain.model.diagram.Layout.LANE;
import static org.setms.km.domain.model.diagram.Placement.IN_MIDDLE;
import static org.setms.km.domain.model.diagram.Placement.NEAR_FROM_VERTEX;
import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.format.Strings.toFriendlyName;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.*;

import java.util.*;
import java.util.stream.Stream;
import lombok.Getter;
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
    var result = new Diagram().setLayout(LANE);
    for (var i = 0; i < sentences.size(); i++) {
      addSentence(i, sentences.get(i), result);
    }
    return result;
  }

  private void addSentence(int index, Sentence sentence, Diagram diagram) {
    var context = new SentenceContext();
    sentence
        .getParts()
        .forEach(
            part -> {
              switch (part.getType()) {
                case "person" -> addBox(part, "material/person", diagram, "actor", context);
                case "people" -> addBox(part, "material/group", diagram, "actor", context);
                case "computerSystem" ->
                    addBox(part, "material/computer", diagram, "actor", context);
                case "activity" ->
                    context.addActivity(index, initLower(toFriendlyName(part.getId())));
                case "workObject" ->
                    addBox(
                        part,
                        Optional.ofNullable(part.getAttributes().get("icon"))
                            .map(List::getFirst)
                            .map(p -> "%s/%s".formatted(p.getType(), initLower(p.getId())))
                            .orElse("material/folder"),
                        diagram,
                        part == sentence.getParts().getLast() ? "workObject" : null,
                        context);
                default ->
                    throw new UnsupportedOperationException(
                        "Can't add to diagram: " + part.getType());
              }
            });
  }

  private void addBox(
      Link part, String iconPath, Diagram diagram, String reuseType, SentenceContext context) {
    var box =
        diagram.add(
            new IconBox(
                part.getId(), "domainStory/" + iconPath, "domainStory/material/questionMark"),
            reuseType);
    Optional.ofNullable(context.addBox(box))
        .map(
            from -> {
              var textPlacements = newTextPlacements();
              textPlacements.put(IN_MIDDLE, context.getActivity());
              context
                  .sequenceNumber()
                  .ifPresent(
                      sequenceNumber -> textPlacements.put(NEAR_FROM_VERTEX, sequenceNumber));
              return new Arrow(from, box, textPlacements, false);
            })
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
          domainStoryResource,
          domainStory,
          extractUseCaseFrom(suggestionCode, inputs),
          determineSystemToDesign(domainStory.getPackage(), inputs.get(DomainStory.class)));
    }
    return unknown(suggestionCode);
  }

  private Optional<String> determineSystemToDesign(
      String packageName, Collection<DomainStory> domainStories) {
    var computerSystems =
        domainStories.stream()
            .filter(domainStory -> domainStory.getPackage().equals(packageName))
            .flatMap(DomainStory::sentences)
            .flatMap(Sentence::parts)
            .filter(Link.testType("computerSystem"))
            .collect(groupingBy(Link::getId));
    return computerSystems.keySet().stream().max(comparing(key -> computerSystems.get(key).size()));
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
      Resource<?> domainStoryResource,
      DomainStory domainStory,
      Optional<UseCase> target,
      Optional<String> systemToDesign) {
    try {
      var converter = new DomainStoryToUseCase(systemToDesign);
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

  private static class SentenceContext {

    private boolean showSequenceNumber = true;
    private String sequenceNumber;
    @Getter private String activity;
    @Getter private Box box;

    public void addActivity(int index, String activity) {
      if (showSequenceNumber) {
        sequenceNumber = Character.toString((char) ('①' + index));
        showSequenceNumber = false;
      } else {
        sequenceNumber = null;
      }
      this.activity = activity;
    }

    public Box addBox(Box box) {
      var result = this.box;
      this.box = box;
      return result;
    }

    public Optional<String> sequenceNumber() {
      return Optional.ofNullable(sequenceNumber);
    }
  }
}
