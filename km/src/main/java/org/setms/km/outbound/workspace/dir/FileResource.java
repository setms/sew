package org.setms.km.outbound.workspace.dir;

import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;

@Getter(AccessLevel.PACKAGE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
class FileResource implements Resource<FileResource> {

  private final File file;
  private final File root;

  FileResource(File file) {
    this(file, file);
  }

  @Override
  public String name() {
    return isRoot() ? "" : file.getName();
  }

  private boolean isRoot() {
    return file.equals(root);
  }

  @Override
  public String path() {
    return isRoot() ? "/" : file.getPath().substring(root.getPath().length());
  }

  @Override
  public URI toUri() {
    return file.toURI();
  }

  @Override
  public Optional<FileResource> parent() {
    return isRoot() ? Optional.empty() : Optional.of(new FileResource(file.getParentFile(), root));
  }

  @Override
  public List<FileResource> children() {
    return Files.childrenOf(file).map(child -> new FileResource(child, root)).toList();
  }

  @Override
  public FileResource select(String path) {
    if (path.startsWith(Resource.SEPARATOR)) {
      return new FileResource(new File(root, path.substring(1)), root);
    }
    try {
      return Optional.of(new File(file, path).getCanonicalFile())
          // Prevent navigating outside the root
          .filter(f -> f.getPath().startsWith(root.getPath()))
          .map(f -> new FileResource(f, root))
          .orElse(null);
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public List<FileResource> matching(Glob glob) {
    return Files.matching(file, glob).stream()
        .map(matching -> new FileResource(matching, root))
        .toList();
  }

  @Override
  public InputStream readFrom() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public OutputStream writeTo() throws IOException {
    file.getParentFile().mkdirs();
    return new FileOutputStream(file);
  }

  @Override
  public void delete() throws IOException {
    Files.delete(file);
  }

  @Override
  public String toString() {
    return file.getPath();
  }
}
