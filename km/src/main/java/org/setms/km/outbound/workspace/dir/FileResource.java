package org.setms.km.outbound.workspace.dir;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.setms.km.domain.model.format.Files;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;

class FileResource implements Resource<FileResource> {

  private final File file;

  FileResource(File file) {
    if (file == null) {
      throw new IllegalArgumentException("Missing file");
    }
    this.file = file;
  }

  @Override
  public String name() {
    return file.getName();
  }

  @Override
  public Optional<FileResource> parent() {
    return Optional.ofNullable(file.getParentFile()).map(FileResource::new);
  }

  @Override
  public List<FileResource> children() {
    return Files.childrenOf(file).map(FileResource::new).toList();
  }

  @Override
  public Collection<FileResource> matching(Glob glob) {
    return FileGlob.matching(file, glob).stream().map(FileResource::new).toList();
  }

  @Override
  public InputStream readFrom() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  public OutputStream writeTo() throws IOException {
    return new FileOutputStream(file);
  }

  @Override
  public void delete() throws IOException {
    Files.delete(file);
  }
}
