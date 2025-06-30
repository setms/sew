package org.setms.sew.core.outbound.tool.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.InputSource;
import org.setms.sew.core.domain.model.tool.OutputSink;

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
