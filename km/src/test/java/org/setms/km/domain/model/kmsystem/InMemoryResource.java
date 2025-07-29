package org.setms.km.domain.model.kmsystem;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;

record InMemoryResource(
    Map<String, byte[]> artifactsByPath,
    String path,
    Consumer<String> pathChanged,
    Consumer<String> pathDeleted)
    implements Resource<InMemoryResource> {

  private static final byte[] EMPTY = new byte[0];

  @Override
  public String name() {
    var index = path.lastIndexOf("/");
    if (index == 0) {
      return path;
    }
    return path.substring(1 + index);
  }

  @Override
  public Optional<InMemoryResource> parent() {
    var index = path.lastIndexOf("/");
    if (index == 0) {
      return Optional.empty();
    }
    return Optional.of(
        new InMemoryResource(artifactsByPath, path.substring(1 + index), pathChanged, pathDeleted));
  }

  @Override
  public List<InMemoryResource> children() {
    return artifactsByPath.keySet().stream()
        .filter(candidate -> candidate.startsWith(path + "/"))
        .map(child -> new InMemoryResource(artifactsByPath, child, pathChanged, pathDeleted))
        .toList();
  }

  @Override
  public InMemoryResource select(String path) {
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
    return new InMemoryResource(artifactsByPath, selected, pathChanged, pathDeleted);
  }

  @Override
  public List<InMemoryResource> matching(Glob glob) {
    return artifactsByPath.keySet().stream()
        .filter(glob::matches)
        .map(match -> new InMemoryResource(artifactsByPath, match, pathChanged, pathDeleted))
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
              pathDeleted.accept(path);
            });
  }
}
