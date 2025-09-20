package org.setms.km.outbound.workspace.memory;

import static java.time.LocalDateTime.now;

import java.io.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;

record InMemoryResource(
    Map<String, byte[]> artifactsByPath,
    Map<String, LocalDateTime> modifiedTimeByPath,
    String path,
    Consumer<String> pathChanged,
    Consumer<String> pathDeleted)
    implements Resource<InMemoryResource> {

  private static final byte[] EMPTY = new byte[0];

  InMemoryResource(String path, Consumer<String> pathChanged, Consumer<String> pathDeleted) {
    this(new TreeMap<>(), new HashMap<>(), path, pathChanged, pathDeleted);
  }

  @Override
  public String name() {
    if (path.equals("/")) {
      return "";
    }
    return path.substring(1 + path.lastIndexOf("/"));
  }

  @Override
  public URI toUri() {
    return URI.create("memory:" + path);
  }

  @Override
  public Optional<InMemoryResource> parent() {
    if (path.equals("/")) {
      return Optional.empty();
    }
    var index = path.lastIndexOf("/");
    if (index == 0) {
      return Optional.of(
          new InMemoryResource(artifactsByPath, modifiedTimeByPath, "/", pathChanged, pathDeleted));
    }
    return Optional.of(
        new InMemoryResource(
            artifactsByPath,
            modifiedTimeByPath,
            path.substring(0, index),
            pathChanged,
            pathDeleted));
  }

  @Override
  public List<InMemoryResource> children() {
    return artifactsByPath.keySet().stream()
        .filter(candidate -> candidate.startsWith(path + "/"))
        .map(this::directChildOf)
        .distinct()
        .map(
            child ->
                new InMemoryResource(
                    artifactsByPath, modifiedTimeByPath, child, pathChanged, pathDeleted))
        .toList();
  }

  private String directChildOf(String descendant) {
    var index = descendant.indexOf("/", path.length() + 1);
    return index < 0 ? descendant : descendant.substring(0, index);
  }

  @Override
  public InMemoryResource select(String path) {
    if (path.startsWith("/")) {
      return new InMemoryResource(
          artifactsByPath, modifiedTimeByPath, path, pathChanged, pathDeleted);
    }
    var selected = this.path;
    for (var part : path.split("/")) {
      if ("..".equals(part)) {
        var index = selected.lastIndexOf("/");
        if (index < 0) {
          return null;
        }
        selected = selected.substring(0, index);
      } else {
        selected = selected.equals("/") ? selected + part : "%s/%s".formatted(selected, part);
      }
    }
    return new InMemoryResource(
        artifactsByPath, modifiedTimeByPath, selected, pathChanged, pathDeleted);
  }

  @Override
  public List<InMemoryResource> matching(String path, String extension) {
    return new LinkedHashSet<>(artifactsByPath.keySet())
        .stream()
            .filter(Glob.of(path, extension)::matches)
            .map(
                match ->
                    new InMemoryResource(
                        artifactsByPath, modifiedTimeByPath, match, pathChanged, pathDeleted))
            .toList();
  }

  @Override
  public InputStream readFrom() {
    return new ByteArrayInputStream(artifactsByPath.getOrDefault(path, EMPTY));
  }

  @Override
  public OutputStream writeTo() {
    return new ByteArrayOutputStream() {
      @Override
      public void close() {
        artifactsByPath.put(path, toByteArray());
        modifiedTimeByPath.put(path, now());
        pathChanged.accept(path);
      }
    };
  }

  @Override
  public void delete() {
    artifactsByPath.keySet().stream()
        .filter(candidate -> candidate.equals(path) || candidate.startsWith(path + "/"))
        .toList()
        .forEach(
            path -> {
              artifactsByPath.remove(path);
              modifiedTimeByPath.remove(path);
              pathDeleted.accept(path);
            });
  }

  @Override
  public LocalDateTime lastModifiedAt() {
    return modifiedTimeByPath.get(path);
  }

  @Override
  public String toString() {
    return toUri().toString();
  }
}
