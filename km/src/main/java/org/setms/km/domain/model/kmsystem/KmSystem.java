package org.setms.km.domain.model.kmsystem;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.Getter;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Format;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.Tools;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Level;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.ArtifactDefinition;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

public class KmSystem {

  private final ObjectMapper mapper = new ObjectMapper();
  @Getter private final Workspace<?> workspace;

  public KmSystem(Workspace<?> workspace) {
    this.workspace = workspace;
    cacheGlobs();
    registerHandlers();
    registerArtifactDefinitions();
    validateArtifacts();
  }

  private void cacheGlobs() {
    Tools.all()
        .map(BaseTool::getAllInputs)
        .flatMap(Collection::stream)
        .map(Input::glob)
        .distinct()
        .forEach(this::cacheGlob);
  }

  private void cacheGlob(Glob glob) {
    var paths = workspace.root().matching(glob).stream().map(Resource::path).collect(toSet());
    writeGlobPaths(resourceContainingPathsFor(glob), paths);
  }

  private Resource<?> resourceContainingPathsFor(Glob glob) {
    return workspace.root().select(".km/globs/%s.glob".formatted(glob));
  }

  private void writeGlobPaths(Resource<? extends Resource<?>> globPath, Collection<String> paths) {
    try (var writer = new PrintWriter(globPath.writeTo())) {
      paths.forEach(writer::println);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to update globs", e);
    }
  }

  public void registerHandlers() {
    workspace.registerArtifactChangedHandler(this::artifactChanged);
    workspace.registerArtifactDeletedHandler(this::artifactDeleted);
  }

  private void artifactChanged(String path, Artifact artifact) {
    addToGlobs(path);
    updateArtifact(path, artifact);
  }

  private void addToGlobs(String path) {
    Tools.all()
        .map(BaseTool::getAllInputs)
        .flatMap(Collection::stream)
        .map(Input::glob)
        .filter(glob -> glob.matches(path))
        .forEach(glob -> addGlobPath(glob, path));
  }

  private void addGlobPath(Glob glob, String path) {
    updateGlobPaths(glob, path, Collection::add);
  }

  private void updateGlobPaths(
      Glob glob, String path, BiFunction<Collection<String>, String, Boolean> updater) {
    var globPath = resourceContainingPathsFor(glob);
    var paths = linesOfTextIn(globPath);
    if (updater.apply(paths, path)) {
      writeGlobPaths(globPath, paths);
    }
  }

  private Collection<String> linesOfTextIn(Resource<? extends Resource<?>> resource) {
    var paths = new TreeSet<String>();
    try (var reader = new BufferedReader(new InputStreamReader(resource.readFrom()))) {
      reader.lines().forEach(paths::add);
    } catch (IOException ignored) {
      // Ignore
    }
    return paths;
  }

  @SuppressWarnings("unchecked")
  private <T extends Artifact> void updateArtifact(String path, T artifact) {
    var aClass = (Class<T>) artifact.getClass();
    updateArtifact(path, aClass, Tools.targeting(aClass));
  }

  private <T extends Artifact> void updateArtifact(
      String path, Class<T> type, Optional<BaseTool<T>> maybeTool) {
    var valid = true;
    var buildResource = reportResourceFor(path);
    if (maybeTool.isPresent()) {
      var tool = maybeTool.get();
      var diagnostics = new LinkedHashSet<Diagnostic>();
      var inputs = resolveInputs(path, tool, diagnostics);
      tool.validate(inputs, diagnostics);
      valid = diagnostics.stream().map(Diagnostic::level).noneMatch(Level.ERROR::equals);
      if (valid) {
        buildReports(tool, buildResource, inputs, diagnostics);
      }
      storeDiagnostics(path, tool, diagnostics);
    }
    if (valid) {
      Tools.dependingOn(type).stream()
          .filter(tool -> maybeTool.isEmpty() || !tool.equals(maybeTool.get()))
          .forEach(
              tool -> {
                var diagnostics = new LinkedHashSet<Diagnostic>();
                var inputs = resolveInputs(path, tool, diagnostics);
                buildReports(tool, buildResource, inputs, diagnostics);
                storeDiagnostics(path, tool, diagnostics);
              });
    }
  }

  private Resource<?> reportResourceFor(String path) {
    return workspace.root().select(".km/reports%s".formatted(path));
  }

  private void buildReports(
      BaseTool<?> tool,
      Resource<? extends Resource<?>> buildResource,
      ResolvedInputs inputs,
      Collection<Diagnostic> diagnostics) {
    try {
      var toolReport = buildResource.select(tool.getClass().getName());
      toolReport.delete();
      tool.build(inputs, toolReport, diagnostics);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to build report", e);
    }
  }

  private ResolvedInputs resolveInputs(
      String path, BaseTool<?> tool, Collection<Diagnostic> diagnostics) {
    var result = new ResolvedInputs();
    resolve(path, tool.getMainInput(), true, diagnostics, result);
    // TODO: Don't add parser errors to diagnostics for this path
    tool.getAdditionalInputs().forEach(input -> resolve(path, input, false, diagnostics, result));
    return result;
  }

  private void resolve(
      String path,
      Input<?> input,
      boolean validate,
      Collection<Diagnostic> diagnostics,
      ResolvedInputs inputs) {
    inputs.put(input.name(), parse(workspace.root(), path, input, validate, diagnostics));
  }

  private <T extends Artifact> List<T> parse(
      Resource<?> resource,
      String path,
      Input<T> input,
      boolean validate,
      Collection<Diagnostic> diagnostics) {
    var parser = input.format().newParser();
    if (input.glob().matches(path)) {
      var result = parser.parse(resource.select(path), input.type(), validate, diagnostics);
      return result == null ? emptyList() : List.of(result);
    }
    return resourcesMatching(input.glob())
        .map(match -> parser.parse(match, input.type(), validate, diagnostics))
        .filter(Objects::nonNull)
        .toList();
  }

  private Stream<Resource<?>> resourcesMatching(Glob glob) {
    return linesOfTextIn(resourceContainingPathsFor(glob)).stream().map(workspace.root()::select);
  }

  private void storeDiagnostics(String path, BaseTool<?> tool, Collection<Diagnostic> diagnostics) {
    var diagnosticsResource =
        workspace
            .root()
            .select(".km/diagnostics%s/%s.json".formatted(path, tool.getClass().getName()));
    try {
      try (var writer = new PrintWriter(diagnosticsResource.writeTo())) {
        mapper.writeValue(writer, serialize(diagnostics));
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

  private void artifactDeleted(String path) {
    removeFromGlobs(path);
  }

  private void removeFromGlobs(String path) {
    Tools.all()
        .map(BaseTool::getAllInputs)
        .flatMap(Collection::stream)
        .map(Input::glob)
        .filter(glob -> glob.matches(path))
        .forEach(glob -> removeGlobPath(glob, path));
  }

  private void removeGlobPath(Glob glob, String path) {
    updateGlobPaths(glob, path, Collection::remove);
  }

  private void registerArtifactDefinitions() {
    Tools.all()
        .map(BaseTool::getMainInput)
        .filter((Objects::nonNull))
        .map(
            input ->
                new ArtifactDefinition(
                    input.type(),
                    input.glob(),
                    Optional.ofNullable(input.format()).map(Format::newParser).orElse(null)))
        .distinct()
        .forEach(workspace::registerArtifactDefinition);
  }

  private void validateArtifacts() {
    new Thread(this::validateExistingArtifacts).start();
  }

  private void validateExistingArtifacts() {
    workspace.root().matching(new Glob(".km/globs", "**/*.glob")).stream()
        .map(this::linesOfTextIn)
        .flatMap(Collection::stream)
        .map(workspace.root()::select)
        .filter(Objects::nonNull)
        .forEach(this::validateExistingArtifact);
  }

  @SuppressWarnings("unchecked")
  private <T extends Artifact> void validateExistingArtifact(Resource<?> artifact) {
    var diagnosticsResources =
        workspace
            .root()
            .matching(new Glob("/.km/diagnostics%s".formatted(artifact.path()), "**/*.json"));
    var lastValidated =
        diagnosticsResources.isEmpty() ? null : diagnosticsResources.getFirst().lastModifiedAt();
    if (lastValidated == null || lastValidated.isBefore(artifact.lastModifiedAt())) {
      Predicate<BaseTool<?>> filter =
          diagnosticsResources.isEmpty()
              ? tool ->
                  tool.getMainInput() != null && tool.getMainInput().glob().matches(artifact.path())
              : tool -> tool.getClass().getName().equals(diagnosticsResources.getFirst().name());
      Tools.all()
          .filter(filter)
          .findFirst()
          .ifPresent(
              tool -> {
                Class<T> artifactType = (Class<T>) tool.getMainInput().type();
                Optional<BaseTool<T>> maybeTool = Optional.of((BaseTool<T>) tool);
                updateArtifact(artifact.path(), artifactType, maybeTool);
              });
    }
  }

  public Resource<?> mainReportFor(String path) {
    var maybeTool =
        Tools.all().filter(tool -> tool.getMainInput().glob().matches(path)).findFirst();
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
  public <T extends Artifact> AppliedSuggestion applySuggestion(
      Resource<?> resource, String code, Location location) {
    if (resource == null) {
      return new AppliedSuggestion();
    }
    return Tools.all()
        .filter(
            tool ->
                tool.getMainInput() != null && tool.getMainInput().glob().matches(resource.path()))
        .findFirst()
        .map(
            tool -> {
              var result = tool.apply(code, workspace, location);
              var type = (Class<T>) tool.getMainInput().type();
              var typedTool = (BaseTool<T>) tool;
              updateArtifact(resource.path(), type, Optional.of(typedTool));
              return result;
            })
        .orElseGet(AppliedSuggestion::new);
  }
}
