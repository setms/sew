package org.setms.km.domain.model.kmsystem;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Format;
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
  private final Workspace workspace;

  public KmSystem(Workspace workspace) {
    this.workspace = workspace;
    this.workspace.registerArtifactChangedHandler(this::artifactChanged);
    this.workspace.registerArtifactDeletedHandler(this::artifactDeleted);
    registerArtifactDefinitions(workspace);
    cacheGlobs();
  }

  private void cacheGlobs() {
    Tools.all()
        .map(BaseTool::getInputs)
        .flatMap(Collection::stream)
        .map(Input::glob)
        .distinct()
        .forEach(this::cacheGlob);
  }

  private void cacheGlob(Glob glob) {
    var paths = workspace.root().matching(glob).stream().map(Resource::path).collect(toSet());
    writeGlobPaths(pathFor(glob), paths);
  }

  private void registerArtifactDefinitions(Workspace workspace) {
    Tools.all()
        .map(BaseTool::getInputs)
        .flatMap(Collection::stream)
        .map(
            input ->
                new ArtifactDefinition(
                    input.type(),
                    input.glob(),
                    Optional.ofNullable(input.format()).map(Format::newParser).orElse(null)))
        .distinct()
        .forEach(workspace::registerArtifactDefinition);
  }

  private void artifactChanged(String path, Artifact artifact) {
    addToGlobs(path);
    updateArtifacts(path, artifact);
  }

  private void addToGlobs(String path) {
    Tools.all()
        .map(BaseTool::getInputs)
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
    var globPath = pathFor(glob);
    var paths = new TreeSet<String>();
    try (var reader = new BufferedReader(new InputStreamReader(globPath.readFrom()))) {
      reader.lines().forEach(paths::add);
    } catch (IOException ignored) {
      // Ignore
    }
    if (updater.apply(paths, path)) {
      writeGlobPaths(globPath, paths);
    }
  }

  private Resource<?> pathFor(Glob glob) {
    return workspace.root().select(".km/globs/%s.glob".formatted(glob));
  }

  private void writeGlobPaths(Resource<? extends Resource<?>> globPath, Collection<String> paths) {
    try (var writer = new PrintWriter(globPath.writeTo())) {
      paths.forEach(writer::println);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to update globs", e);
    }
  }

  private void updateArtifacts(String path, Artifact artifact) {
    var maybeTool = Tools.targeting(artifact.getClass());
    var valid = true;
    var buildResource = reportResourceFor(path);
    if (maybeTool.isPresent()) {
      var tool = maybeTool.get();
      var diagnostics = new LinkedHashSet<Diagnostic>();
      var inputs = resolveInputs(tool, diagnostics);
      tool.validate(inputs, diagnostics);
      valid = diagnostics.stream().map(Diagnostic::level).noneMatch(Level.ERROR::equals);
      if (valid) {
        tool.build(inputs, buildResource.select(tool.getClass().getName()), diagnostics);
      }
      storeDiagnostics(path, tool, diagnostics);
    }
    if (valid) {
      Tools.dependingOn(artifact.getClass()).stream()
          .filter(tool -> maybeTool.isEmpty() || !tool.equals(maybeTool.get()))
          .forEach(
              tool -> {
                var diagnostics = new LinkedHashSet<Diagnostic>();
                var inputs = resolveInputs(tool, diagnostics);
                tool.build(inputs, buildResource.select(tool.getClass().getName()), diagnostics);
                storeDiagnostics(path, tool, diagnostics);
              });
    }
  }

  private Resource<?> reportResourceFor(String path) {
    return workspace.root().select(".km/reports%s".formatted(path));
  }

  private ResolvedInputs resolveInputs(BaseTool tool, Collection<Diagnostic> diagnostics) {
    var result = new ResolvedInputs();
    var validate = new AtomicBoolean(true);
    tool.getInputs()
        .forEach(
            input ->
                result.put(
                    input.name(),
                    parse(workspace.root(), input, validate.getAndSet(false), diagnostics)));
    return result;
  }

  private <T extends Artifact> List<T> parse(
      Resource<?> resource, Input<T> input, boolean validate, Collection<Diagnostic> diagnostics) {
    return input
        .format()
        .newParser()
        .parseMatching(resource, input.glob(), input.type(), validate, diagnostics)
        .toList();
  }

  private void storeDiagnostics(String path, BaseTool tool, Collection<Diagnostic> diagnostics) {
    var diagnosticsResource =
        workspace
            .root()
            .select(".km/diagnostics%s/%s.json".formatted(path, tool.getClass().getName()));
    try {
      if (diagnostics.isEmpty()) {
        diagnosticsResource.delete();
      } else {
        try (var writer = new PrintWriter(diagnosticsResource.writeTo())) {
          mapper.writeValue(writer, serialize(diagnostics));
        }
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

  public List<Diagnostic> diagnosticsFor(String path) {
    var result = new ArrayList<Diagnostic>();
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

  public Resource<?> mainReportFor(String path) {
    var maybeTool =
        Tools.all().filter(tool -> tool.getInputs().getFirst().glob().matches(path)).findFirst();
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

  private void artifactDeleted(String path) {
    removeFromGlobs(path);
  }

  private void removeFromGlobs(String path) {
    Tools.all()
        .map(BaseTool::getInputs)
        .flatMap(Collection::stream)
        .map(Input::glob)
        .filter(glob -> glob.matches(path))
        .forEach(glob -> removeGlobPath(glob, path));
  }

  private void removeGlobPath(Glob glob, String path) {
    updateGlobPaths(glob, path, Collection::remove);
  }
}
