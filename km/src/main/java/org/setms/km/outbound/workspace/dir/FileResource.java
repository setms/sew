package org.setms.km.outbound.workspace.dir;

import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.format.Files;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;

@Getter(AccessLevel.PACKAGE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class FileResource implements Resource<FileResource> {

  private final File file;
  private final File root;

  FileResource(File file) {
    if (file == null) {
      throw new IllegalArgumentException("Missing file");
    }
    this.file = file;
    this.root = file;
  }

  @Override
  public String name() {
    return file.getName();
  }

  @Override
  public URI toUri() {
    return file.toURI();
  }

  @Override
  public Optional<FileResource> parent() {
    if (file.equals(root)) {
      return Optional.empty();
    }
    return Optional.ofNullable(file.getParentFile()).map(parent -> new FileResource(parent, root));
  }

  @Override
  public List<FileResource> children() {
    return Files.childrenOf(file).map(child -> new FileResource(child, root)).toList();
  }

  @Override
  public FileResource select(String path) {
    if (path.startsWith(Resource.SEPARATOR)) {
      if (!path.startsWith(root().path())) {
        // Prevent navigating outside the root
        return null;
      }
      return new FileResource(new File(root, path.substring(root().path().length())), root);
    }
    return Optional.of(new File(file, path))
        // Prevent navigating outside the root
        .filter(f -> f.getPath().startsWith(root.getPath()))
        .map(f -> new FileResource(f, root))
        .orElse(null);
  }

  @Override
  public List<FileResource> matching(Glob glob) {
    return FileGlob.matching(file, glob).stream()
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
}
