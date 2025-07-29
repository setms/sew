package org.setms.km.domain.model.kmsystem;

import static java.util.stream.Collectors.toSet;

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
import org.setms.km.domain.model.workspace.ArtifactDefinition;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

public class KmSystem {

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
    updateArtifacts(artifact);
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

  private void updateArtifacts(Artifact artifact) {
    var maybeTool = Tools.targeting(artifact.getClass());
    var valid = true;
    if (maybeTool.isPresent()) {
      var tool = maybeTool.get();
      var diagnostics = new LinkedHashSet<Diagnostic>();
      var inputs = resolveInputs(tool, diagnostics);
      tool.validate(inputs, diagnostics);
      valid = diagnostics.stream().map(Diagnostic::level).noneMatch(Level.ERROR::equals);
      if (valid) {
        tool.build(inputs, workspace.root().select("build"), diagnostics);
      }
    }
    if (valid) {
      Tools.dependingOn(artifact.getClass())
          .forEach(
              tool -> {
                var diagnostics = new LinkedHashSet<Diagnostic>();
                var inputs = resolveInputs(tool, diagnostics);
                tool.build(inputs, workspace.root().select("build"), diagnostics);
              });
    }
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
