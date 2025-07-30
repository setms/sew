package org.setms.km.outbound.workspace.dir;

import static io.methvin.watcher.DirectoryChangeEvent.EventType.DELETE;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

@Slf4j
public class DirectoryWorkspace extends Workspace {

  private final DirectoryWatcher watcher;
  final File root;

  public DirectoryWorkspace(File root) {
    this.root = validate(root);
    try {
      this.watcher =
          DirectoryWatcher.builder().path(this.root.toPath()).listener(this::fileChanged).build();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to watch directory " + root, e);
    }
    this.watcher.watchAsync();
    // https://github.com/gmethvin/directory-watcher/issues/87
    try {
      Thread.sleep(25);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private static File validate(File file) {
    if (file == null) {
      throw new IllegalArgumentException("Missing directory");
    }
    try {
      var result = file.getCanonicalFile();
      if (!result.isDirectory()) {
        result.mkdirs();
      }
      return result;
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  void fileChanged(DirectoryChangeEvent event) {
    if (event.isDirectory()) {
      return;
    }
    var path = toPath(event);
    if (event.eventType() == DELETE) {
      onDeleted(path);
    } else {
      parse(path).ifPresent(artifact -> onChanged(path, artifact));
    }
  }

  private String toPath(DirectoryChangeEvent event) {
    var result = event.path().toString();
    result = result.substring(root.getPath().length());
    return result;
  }

  @Override
  protected Resource<?> newRoot() {
    return new FileResource(root, this);
  }

  @Override
  public void close() throws IOException {
    watcher.close();
    super.close();
  }
}
