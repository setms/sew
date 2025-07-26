package org.setms.km.outbound.workspace.dir;

import static io.methvin.watcher.DirectoryChangeEvent.EventType.DELETE;

import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryWatcher;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.setms.km.domain.model.workspace.InputSource;
import org.setms.km.domain.model.workspace.OutputSink;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

@Slf4j
public class DirectoryWorkspace extends Workspace {

  private final File inputDirectory;
  private final File outputDirectory;
  private final DirectoryWatcher watcher;

  public DirectoryWorkspace(File directory) {
    this(directory, new File(directory, "build"));
  }

  public DirectoryWorkspace(File inputDirectory, File outputDirectory) {
    this.inputDirectory = validate(inputDirectory);
    this.outputDirectory =
        Optional.ofNullable(outputDirectory)
            .map(DirectoryWorkspace::validate)
            .orElseGet(DirectoryWorkspace::tempDir);
    try {
      this.watcher =
          DirectoryWatcher.builder()
              .path(this.inputDirectory.toPath())
              .listener(this::fileChanged)
              .build();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to watch directory " + inputDirectory, e);
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
    parse(event.path().toString()).ifPresent(this::onChanged);
  }

  @Override
  protected InputSource newInputSource() {
    return new FileInputSource(inputDirectory);
  }

  @Override
  protected OutputSink newOutputSink() {
    return new FileOutputSink(outputDirectory);
  }

  @Override
  protected Resource newRoot() {
    return new FileResource(inputDirectory);
  }

  @Override
  public void close() throws IOException {
    watcher.close();
    super.close();
  }
}
