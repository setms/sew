package org.setms.km.outbound.workspace.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.format.Files;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.InputSource;
import org.setms.km.domain.model.workspace.OutputSink;

@Getter
@RequiredArgsConstructor
class FileOutputSink implements OutputSink {

  private final File file;

  @Override
  public FileOutputSink select(String path) {
    try {
      return new FileOutputSink(new File(file, path).getCanonicalFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    Files.delete(file);
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
