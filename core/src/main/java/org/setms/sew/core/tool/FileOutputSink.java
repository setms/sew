package org.setms.sew.core.tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FileOutputSink implements OutputSink {

  private final File file;

  public FileOutputSink() {
    this(tempDir());
  }

  private static File tempDir() {
    try {
      return Files.createTempDirectory("sew").toFile();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public FileOutputSink select(String path) {
    return new FileOutputSink(new File(file, path));
  }

  @Override
  public List<? extends OutputSink> matching(Glob glob) {
    return FileGlob.matching(file, glob).stream().map(FileOutputSink::new).toList();
  }

  @Override
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public OutputStream open() throws IOException {
    file.getParentFile().mkdirs();
    return new FileOutputStream(file);
  }

  @Override
  public URI toUri() {
    return file.toURI();
  }

  @Override
  public List<FileOutputSink> containers() {
    return Optional.ofNullable(file.listFiles()).stream()
        .flatMap(Arrays::stream)
        .filter(File::isDirectory)
        .filter(d -> !d.getName().startsWith("."))
        .map(FileOutputSink::new)
        .toList();
  }

  @Override
  public void delete() throws IOException {
    Files.delete(file.toPath());
  }

  @Override
  public InputSource toInput() {
    return new FileInputSource(file);
  }

  @Override
  public String toString() {
    return toUri().toString();
  }
}
