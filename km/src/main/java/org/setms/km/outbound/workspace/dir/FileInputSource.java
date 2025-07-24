package org.setms.km.outbound.workspace.dir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.InputSource;

@Getter
@RequiredArgsConstructor
class FileInputSource implements InputSource {

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
  public URI toUri() {
    return file.toURI();
  }

  @Override
  public InputStream open() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  public InputSource select(String path) {
    try {
      return new FileInputSource(toFile(path).getCanonicalFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private File toFile(String path) {
    return path.startsWith(File.separator) ? new File(path) : new File(file, path);
  }

  @Override
  public String toString() {
    return file.toURI().toString();
  }
}
