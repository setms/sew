package org.setms.km.outbound.workspace.dir;

import static io.methvin.watcher.DirectoryChangeEvent.EventType.DELETE;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

@Slf4j
public class DirectoryWorkspace extends Workspace {

  private final File root;
  private final DirectoryWatcher watcher;

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

  private static File tempDir() {
    try {
      return Files.createTempDirectory("org-setms-km").toFile();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private void fileChanged(DirectoryChangeEvent event) {
    if (event.isDirectory() || event.eventType() == DELETE) {
      return;
    }
    var path = event.path().toString();
    path = path.substring(root.getParent().length());
    parse(path).ifPresent(this::onChanged);
  }

  @Override
  protected Resource<?> newRoot() {
    return new FileResource(root);
  }

  @Override
  public void close() throws IOException {
    watcher.close();
    super.close();
  }
}
