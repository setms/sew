package org.setms.km.outbound.workspace.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.InputSource;
import org.setms.km.domain.model.workspace.OutputSink;

@Getter
@RequiredArgsConstructor
public class FileInputSource implements InputSource {

  private final File file;

  @Override
  public Collection<FileInputSource> matching(Glob glob) {
    return FileGlob.matching(file, glob).stream().map(FileInputSource::new).toList();
  }

  @Override
  public String name() {
    return file.getName();
  }

  @Override
  public InputStream open() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  public OutputSink toSink() {
    return new FileOutputSink(file);
  }

  @Override
  public String toString() {
    return file.toURI().toString();
  }
}
