package org.setms.km.domain.model.orchestration;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.setms.km.domain.model.tool.AppliedSuggestion.none;
import static org.setms.km.domain.model.validation.Level.ERROR;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.StandaloneTool;
import org.setms.km.domain.model.tool.Tool;
import org.setms.km.domain.model.tool.Tools;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Level;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.ArtifactDefinition;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public class ProcessOrchestrator {

  private static final String INPUTS = "inputs";
  private static final String PATHS = "paths";
  public static final LocalDateTime LONG_AGO = LocalDateTime.of(0, 1, 1, 0, 0);

  private final ObjectMapper mapper = new JsonMapper();
  @Getter private final Workspace<?> workspace;

  public ProcessOrchestrator(Workspace<?> workspace) {
    this.workspace = workspace;
    cacheInputs();
    registerHandlers();
    registerArtifactDefinitions();
    validateArtifactsInBackground();
  }

  private void cacheInputs() {
    Tools.all()
        .map(Tool::allInputs)
        .flatMap(Collection::stream)
        .distinct()
        .forEach(this::cacheInput);
  }

  private void cacheInput(Input<?> input) {
    var paths =
        workspace.root().matching(input.path(), input.extension()).stream()
            .map(Resource::path)
            .collect(toSet());
    writeInputPaths(resourceContainingPathsFor(input), paths);
  }

  private Resource<?> resourceContainingPathsFor(Input<?> input) {
    return workspace
        .root()
        .select(".km/%s/%s/%s.%s".formatted(INPUTS, input.path(), input.extension(), PATHS));
  }

  private void writeInputPaths(
      Resource<? extends Resource<?>> inputResource, Collection<String> paths) {
    try (var writer = new PrintWriter(inputResource.writeTo())) {
      paths.forEach(writer::println);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to update input paths", e);
    }
  }

  private void registerHandlers() {
    workspace.registerArtifactChangedHandler(this::artifactChanged);
    workspace.registerArtifactDeletedHandler(this::artifactDeleted);
  }

  private void artifactChanged(String path, Artifact artifact) {
    if (isInternalResource(path)) {
      return;
    }
    addToInputs(path);
    validateAndBuildArtifact(path, artifact);
  }

  private boolean isInternalResource(String path) {
    return path.startsWith("/.km/");
  }

  private void addToInputs(String path) {
    Tools.all()
        .map(Tool::allInputs)
        .flatMap(Collection::stream)
        .distinct()
        .filter(input -> input.matches(path))
        .forEach(input -> addInputPath(input, path));
  }

  private void addInputPath(Input<?> input, String path) {
    updateInputPaths(input, path, Collection::add);
  }

  private void updateInputPaths(
      Input<?> input, String path, BiFunction<Collection<String>, String, Boolean> updater) {
    var globPath = resourceContainingPathsFor(input);
    var paths = linesOfTextIn(globPath);
    if (updater.apply(paths, path)) {
      writeInputPaths(globPath, paths);
    }
  }

  private Collection<String> linesOfTextIn(Resource<? extends Resource<?>> resource) {
    var result = new TreeSet<String>();
    try (var reader = new BufferedReader(new InputStreamReader(resource.readFrom()))) {
      reader.lines().forEach(result::add);
    } catch (IOException _) {
      // Ignore
    }
    return result;
  }

  private void validateAndBuildArtifact(String path, Artifact artifact) {
    var valid = validate(path, artifact);
    revalidateArtifactsThatDependOn(path);
    if (valid) {
      var buildResource = reportResourceFor(path);
      rebuildReportsThatDependOn(artifact, buildResource);
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends Artifact> boolean validate(String path, Artifact artifact) {
    var result = true;
    for (var tool : Tools.validating(artifact.getClass())) {
      var inputs = resolveInputs(tool.validationContext());
      var diagnostics = new LinkedHashSet<Diagnostic>();
      var typedTool = (ArtifactTool<T>) tool;
      var typedArtifact = (T) artifact;
      typedTool.validate(typedArtifact, inputs, diagnostics);
      storeDiagnostics(path, tool, diagnostics);
      if (result && diagnostics.stream().map(Diagnostic::level).anyMatch(ERROR::equals)) {
        result = false;
      }
    }
    return result;
  }

  private ResolvedInputs resolveInputs(Set<Input<? extends Artifact>> inputs) {
    var result = new ResolvedInputs();
    var diagnostics = new ArrayList<Diagnostic>();
    inputs.forEach(
        input -> {
          var parser = input.format().newParser();
          result.put(
              input.name(),
              resourcesMatching(input)
                  .map(resource -> parser.parse(resource, input.type(), false, diagnostics))
                  .filter(Objects::nonNull)
                  .toList());
        });
    return result;
  }

  private Resource<?> reportResourceFor(String path) {
    return workspace.root().select(".km/reports%s".formatted(path));
  }

  private void rebuildReportsThatDependOn(Artifact artifact, Resource<?> buildResource) {
    deleteReports(buildResource);
    Tools.buildingReportsFor(artifact.getClass())
        .forEach(
            tool -> {
              var inputs = resolveInputs(tool.reportingContext());
              var diagnostics = new LinkedHashSet<Diagnostic>();
              var output = buildResource.select(tool.getClass().getName());
              switch (tool) {
                case ArtifactTool<?> artifactTool ->
                    buildReportsFor(artifact, artifactTool, inputs, output, diagnostics);
                case StandaloneTool standaloneTool ->
                    standaloneTool.buildReports(inputs, output, diagnostics);
              }
            });
  }

  private void deleteReports(Resource<?> buildResource) {
    try {
      buildResource.delete();
    } catch (IOException e) {
      log.error("Failed to delete reports at {}", buildResource.path());
    }
  }

  @SuppressWarnings("unchecked")
  private <A extends Artifact> void buildReportsFor(
      Artifact artifact,
      ArtifactTool<A> artifactTool,
      ResolvedInputs inputs,
      Resource<? extends Resource<?>> output,
      LinkedHashSet<Diagnostic> diagnostics) {
    artifactTool
        .reportingTarget()
        .map(Input::type)
        .filter(artifact.getClass()::equals)
        .ifPresentOrElse(
            _ -> {
              A typedArtifact = (A) artifact;
              artifactTool.buildReportsFor(typedArtifact, inputs, output, diagnostics);
            },
            () ->
                artifactTool
                    .reportingTarget()
                    .ifPresent(
                        input ->
                            resourcesMatching(input)
                                .forEach(
                                    reportResource -> {
                                      var reportArtifact = (A) parse(reportResource.path(), input);
                                      artifactTool.buildReportsFor(
                                          reportArtifact, inputs, output, diagnostics);
                                    })));
  }

  private Stream<Resource<?>> resourcesMatching(Input<?> input) {
    return linesOfTextIn(resourceContainingPathsFor(input)).stream().map(workspace.root()::select);
  }

  private void storeDiagnostics(String path, Tool tool, Collection<Diagnostic> diagnostics) {
    var diagnosticsResource =
        workspace
            .root()
            .select(".km/diagnostics%s/%s.json".formatted(path, tool.getClass().getName()));
    try {
      try (var writer = new PrintWriter(diagnosticsResource.writeTo())) {
        writer.println(new JSONObject(serialize(diagnostics)).toString(2));
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to store diagnostics", e);
    }
  }

  private Map<String, List<Map<String, Object>>> serialize(Collection<Diagnostic> diagnostics) {
    return Map.of("diagnostics", diagnostics.stream().map(this::serialize).toList());
  }

  private Map<String, Object> serialize(Diagnostic diagnostic) {
    var result = new HashMap<String, Object>();
    result.put("level", diagnostic.level().toString());
    result.put("message", diagnostic.message());
    if (diagnostic.location() != null) {
      result.put("location", diagnostic.location().toString());
    }
    if (!diagnostic.suggestions().isEmpty()) {
      result.put("suggestions", diagnostic.suggestions().stream().map(this::serialize).toList());
    }
    return result;
  }

  private Map<String, String> serialize(Suggestion suggestion) {
    return Map.of("code", suggestion.code(), "message", suggestion.message());
  }

  public Set<Diagnostic> diagnosticsFor(String path) {
    var result = new LinkedHashSet<Diagnostic>();
    workspace
        .root()
        .select(".km/diagnostics%s".formatted(path))
        .children()
        .forEach(diagnosticResource -> result.addAll(deserializeFrom(diagnosticResource)));
    return result;
  }

  @SuppressWarnings("unchecked")
  private Collection<Diagnostic> deserializeFrom(Resource<?> diagnosticResource) {
    try (var input = diagnosticResource.readFrom()) {
      var diagnostics = (Map<String, List<Map<String, Object>>>) mapper.readValue(input, Map.class);
      return diagnostics.get("diagnostics").stream().map(this::deserialize).toList();
    } catch (Exception e) {
      return emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  private Diagnostic deserialize(Map<String, Object> diagnostic) {
    var level = Level.valueOf((String) diagnostic.get("level"));
    var message = diagnostic.get("message").toString();
    var location =
        Optional.ofNullable(diagnostic.get("location"))
            .map(String.class::cast)
            .map(path -> path.split("/"))
            .map(Location::new)
            .orElse(null);
    var suggestions =
        Optional.ofNullable(diagnostic.get("suggestions")).map(Collection.class::cast).stream()
            .flatMap(Collection::stream)
            .map(suggestion -> deserializeSuggestion((Map<String, String>) suggestion))
            .toList();
    return new Diagnostic(level, message, location, suggestions);
  }

  private Suggestion deserializeSuggestion(Map<String, String> suggestion) {
    return new Suggestion(suggestion.get("code"), suggestion.get("message"));
  }

  public Set<Diagnostic> diagnostics() {
    return workspace.root().matching(".km/diagnostics", "json").stream()
        .map(this::deserializeFrom)
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  public Set<Diagnostic> diagnosticsWithSuggestions() {
    return workspace.root().matching(".km/diagnostics", "json").stream()
        .map(this::deserializeFrom)
        .flatMap(Collection::stream)
        .filter(Diagnostic::hasSuggestion)
        .collect(toSet());
  }

  private void artifactDeleted(String path) {
    if (isInternalResource(path)) {
      return;
    }
    removeFromInputs(path);
    deleteInternalResourcesReferencing(path);
    revalidateArtifactsThatDependOn(path);
  }

  private void deleteInternalResourcesReferencing(String path) {
    Stream.of("diagnostics", "reports")
        .map(dir -> ".km/%s%s".formatted(dir, path))
        .map(workspace.root()::select)
        .forEach(this::deleteIgnoreExceptions);
  }

  private void deleteIgnoreExceptions(Resource<?> resource) {
    try {
      resource.delete();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void removeFromInputs(String path) {
    Tools.all()
        .map(Tool::allInputs)
        .flatMap(Collection::stream)
        .filter(input -> input.matches(path))
        .forEach(input -> removeInputPath(input, path));
  }

  private void removeInputPath(Input<?> input, String path) {
    updateInputPaths(input, path, Collection::remove);
  }

  private void revalidateArtifactsThatDependOn(String path) {
    Tools.all()
        .filter(tool -> tool.validationContext().stream().anyMatch(input -> input.matches(path)))
        .forEach(tool -> validateDependent(tool, path));
  }

  private void validateDependent(Tool tool, String path) {
    var resolvedInputs = resolveInputs(tool.validationContext());
    switch (tool) {
      case ArtifactTool<?> artifactTool -> validateDependent(path, artifactTool, resolvedInputs);
      case StandaloneTool standaloneTool -> {
        var diagnostics = new LinkedHashSet<Diagnostic>();
        standaloneTool.validate(resolvedInputs, diagnostics);
        storeDiagnostics(path, tool, diagnostics);
      }
    }
  }

  private void validateDependent(
      String path, ArtifactTool<?> artifactTool, ResolvedInputs resolvedInputs) {
    if (artifactTool.validationTarget().matches(path)) {
      var diagnostics = new LinkedHashSet<Diagnostic>();
      artifactTool.validate(workspace.root().select(path), resolvedInputs, diagnostics);
      storeDiagnostics(path, artifactTool, diagnostics);
    } else {
      resourcesMatching(artifactTool.validationTarget())
          .forEach(
              resource -> {
                var diagnostics = new LinkedHashSet<Diagnostic>();
                artifactTool.validate(resource, resolvedInputs, diagnostics);
                storeDiagnostics(resource.path(), artifactTool, diagnostics);
              });
    }
  }

  private void registerArtifactDefinitions() {
    Tools.all()
        .map(Tool::allInputs)
        .flatMap(Collection::stream)
        .map(
            input ->
                new ArtifactDefinition(
                    input.type(),
                    Glob.of(input.path(), input.extension()),
                    Optional.ofNullable(input.format()).map(Format::newParser).orElse(null)))
        .distinct()
        .forEach(workspace::registerArtifactDefinition);
  }

  protected void validateArtifactsInBackground() {
    var thread = new Thread(this::validateExistingArtifacts);
    thread.setDaemon(true);
    thread.start();
  }

  protected void validateExistingArtifacts() {
    outOfDateArtifacts().forEach(this::updateOutOfDateArtifact);
  }

  protected List<OutOfDateArtifact> outOfDateArtifacts() {
    return workspace.root().matching(".km/" + INPUTS, PATHS).stream()
        .map(this::linesOfTextIn)
        .flatMap(Collection::stream)
        .toList()
        .stream()
        .map(workspace.root()::select)
        .filter(Objects::nonNull)
        .flatMap(this::checkOutOfDate)
        .toList();
  }

  private Stream<OutOfDateArtifact> checkOutOfDate(Resource<?> artifact) {
    return Stream.of(artifact)
        .filter(a -> isBefore(lastValidated(a.path()), a.lastModifiedAt()))
        .map(Resource::path)
        .map(OutOfDateArtifact::new);
  }

  private LocalDateTime lastValidated(String path) {
    return workspace.root().matching("/.km/diagnostics%s".formatted(path), "json").stream()
        .map(Resource::lastModifiedAt)
        .min(LocalDateTime::compareTo)
        .orElse(LONG_AGO);
  }

  private boolean isBefore(LocalDateTime dt1, LocalDateTime dt2) {
    return dt1 == null || dt2 == null || dt1.isBefore(dt2);
  }

  protected void updateOutOfDateArtifact(OutOfDateArtifact outOfDate) {
    var path = outOfDate.path();
    Tools.all()
        .map(Tool::allInputs)
        .flatMap(Collection::stream)
        .distinct()
        .filter(input -> input.matches(path))
        .map(input -> parse(path, input))
        .filter(Objects::nonNull)
        .findFirst()
        .ifPresent(artifact -> artifactChanged(path, artifact));
  }

  private Artifact parse(String path, Input<? extends Artifact> input) {
    return input
        .format()
        .newParser()
        .parse(workspace.root().select(path), input.type(), false, new LinkedHashSet<>());
  }

  public Resource<?> mainReportFor(String path) {
    var maybeTool =
        Tools.all()
            .filter(ArtifactTool.class::isInstance)
            .map(ArtifactTool.class::cast)
            .filter(tool -> tool.reportsOn(path))
            .findFirst();
    if (maybeTool.isEmpty()) {
      return null;
    }
    var result = reportResourceFor(path).select(maybeTool.get().getClass().getName());
    if (result.children().isEmpty()) {
      return null;
    }
    if (result.children().stream().map(Resource::name).anyMatch(name -> name.contains("."))) {
      return result;
    }
    return result.children().getFirst();
  }

  @SuppressWarnings("unchecked")
  public AppliedSuggestion applySuggestion(Resource<?> resource, String code, Location location) {
    if (resource == null) {
      return none();
    }
    for (var tool :
        Tools.all()
            .filter(ArtifactTool.class::isInstance)
            .map(ArtifactTool.class::cast)
            .filter(t -> t.validates(resource.path()))
            .toList()) {
      var inputs = resolveInputs(tool.validationContext());
      var artifact = parse(resource.path(), tool.validationTarget());
      if (artifact == null) {
        continue;
      }
      var result = tool.applySuggestion(artifact, code, location, inputs, resource);
      if (!result.createdOrChanged().isEmpty()) {
        return result;
      }
    }
    return none();
  }
}
