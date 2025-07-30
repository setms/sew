package org.setms.km.outbound.workspace.dir;

import static io.methvin.watcher.DirectoryChangeEvent.EventType.*;

import io.methvin.watcher.DirectoryChangeEvent;
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
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode
class FileResource implements Resource<FileResource> {

  private final File file;
  private final DirectoryWorkspace workspace;

  @Override
  public String name() {
    return isRoot() ? "" : file.getName();
  }

  private boolean isRoot() {
    return file.equals(workspace.root);
  }

  @Override
  public String path() {
    return isRoot() ? "/" : file.getPath().substring(workspace.root.getPath().length());
  }

  @Override
  public URI toUri() {
    return file.toURI();
  }

  @Override
  public Optional<FileResource> parent() {
    return isRoot()
        ? Optional.empty()
        : Optional.of(new FileResource(file.getParentFile(), workspace));
  }

  @Override
  public List<FileResource> children() {
    return Files.childrenOf(file).map(child -> new FileResource(child, workspace)).toList();
  }

  @Override
  public FileResource select(String path) {
    if (path.startsWith(Resource.SEPARATOR)) {
      return new FileResource(new File(workspace.root, path.substring(1)), workspace);
    }
    try {
      return Optional.of(new File(file, path).getCanonicalFile())
          // Prevent navigating outside the root
          .filter(f -> f.getPath().startsWith(workspace.root.getPath()))
          .map(f -> new FileResource(f, workspace))
          .orElse(null);
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public List<FileResource> matching(Glob glob) {
    return Files.matching(file, glob).stream()
        .map(matching -> new FileResource(matching, workspace))
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
    var eventType = file.isFile() ? MODIFY : CREATE;
    return new FileOutputStream(file) {
      @Override
      public void close() throws IOException {
        super.close();
        // For some reason, the directory watcher doesn't pick up the file changes
        workspace.fileChanged(
            new DirectoryChangeEvent(eventType, false, file.toPath(), null, 1, null));
      }
    };
  }

  @Override
  public void delete() throws IOException {
    var path = file.toPath();
    Files.delete(file);
    workspace.fileChanged(new DirectoryChangeEvent(DELETE, false, path, null, 1, null));
  }

  @Override
  public String toString() {
    return file.getPath();
  }
}
