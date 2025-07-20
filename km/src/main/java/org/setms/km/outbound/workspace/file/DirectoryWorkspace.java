package org.setms.km.outbound.workspace.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import org.setms.km.domain.model.workspace.InputSource;
import org.setms.km.domain.model.workspace.OutputSink;
import org.setms.km.domain.model.workspace.Workspace;

public class DirectoryWorkspace extends Workspace {

  private final File inputDirectory;
  private final File outputDirectory;

  public DirectoryWorkspace(File directory) {
    this(directory, new File(directory, "build"));
  }

  public DirectoryWorkspace(File inputDirectory, File outputDirectory) {
    this.inputDirectory = validate(inputDirectory);
    this.outputDirectory =
        Optional.ofNullable(outputDirectory)
            .map(DirectoryWorkspace::validate)
            .orElseGet(DirectoryWorkspace::tempDir);
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

  @Override
  protected InputSource newInputSource() {
    return new FileInputSource(inputDirectory);
  }

  @Override
  protected OutputSink newOutputSink() {
    return new FileOutputSink(outputDirectory);
  }
}
