package org.setms.sew.intellij.plugin.workspace;

import com.intellij.openapi.vfs.VirtualFile;
import java.io.*;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;

class VirtualFileResource implements Resource<VirtualFileResource> {

  private final VirtualFile virtualFile;
  private final File file;
  private final String rootPath;

  VirtualFileResource(VirtualFile virtualFile, File file, String rootPath) {
    if (virtualFile == null && file == null) {
      throw new IllegalArgumentException("Need either VirtualFile or File");
    }
    this.virtualFile = virtualFile;
    this.file = file;
    this.rootPath = rootPath;
  }

  @Override
  public String name() {
    return virtualFile == null ? file.getName() : virtualFile.getName();
  }

  @Override
  public String path() {
    var filePath = virtualFile == null ? file.getPath() : virtualFile.getPath();
    if (!filePath.startsWith(rootPath)) {
      return "<outside workspace>";
    }
    return filePath.equals(rootPath) ? "/" : filePath.substring(rootPath.length());
  }

  @Override
  public Optional<VirtualFileResource> parent() {
    if (virtualFile == null) {
      return Optional.ofNullable(file.getParentFile())
          .map(parent -> new VirtualFileResource(null, parent, rootPath));
    }
    return Optional.ofNullable(virtualFile.getParent())
        .map(parent -> new VirtualFileResource(parent, null, rootPath));
  }

  @Override
  public List<VirtualFileResource> children() {
    if (virtualFile == null) {
      return Files.childrenOf(file)
          .map(child -> new VirtualFileResource(null, child, rootPath))
          .toList();
    }
    return Stream.ofNullable(virtualFile.getChildren())
        .flatMap(Arrays::stream)
        .map(child -> new VirtualFileResource(child, null, rootPath))
        .toList();
  }

  @Override
  public VirtualFileResource select(String path) {
    if (path.startsWith(File.separator)) {
      var found =
          virtualFile == null
              ? null
              : virtualFile.getFileSystem().refreshAndFindFileByPath(rootPath + path);
      return found == null
          ? new VirtualFileResource(null, new File(rootPath + path), rootPath)
          : new VirtualFileResource(found, null, rootPath);
    }
    if (virtualFile == null) {
      return new VirtualFileResource(null, new File(file, path), rootPath);
    }
    VirtualFile result = null;
    try {
      result = virtualFile.findFileByRelativePath(path);
    } catch (Exception _) {
      // Nothing to do
    }
    if (result != null) {
      return new VirtualFileResource(result, null, rootPath);
    }
    return new VirtualFileResource(
        null, new File(virtualFile.toNioPath().toFile(), path), rootPath);
  }

  @Override
  public List<VirtualFileResource> matching(String path, String extension) {
    if (virtualFile == null) {
      return Files.matching(file, Glob.of(path, extension)).stream()
          .map(found -> new VirtualFileResource(null, found, rootPath))
          .toList();
    }
    return new VirtualFileResource(null, virtualFile.toNioPath().toFile(), rootPath)
        .matching(path, extension);
  }

  @Override
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public InputStream readFrom() throws IOException {
    if (virtualFile == null) {
      file.getParentFile().mkdirs();
      if (!file.isFile()) {
        throw new IOException("%s is not a file, so can't read from it".formatted(file));
      }
      return new FileInputStream(file);
    }
    if (virtualFile.isDirectory()) {
      throw new IOException("Can't read from directory " + virtualFile.toNioPath());
    }
    return virtualFile.getInputStream();
  }

  @Override
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public OutputStream writeTo() throws IOException {
    if (virtualFile == null) {
      file.getParentFile().mkdirs();
      return new FileOutputStream(file);
    }
    return virtualFile.getOutputStream(null);
  }

  @Override
  public void delete() throws IOException {
    if (virtualFile == null) {
      Files.delete(file);
    } else {
      virtualFile.delete(null);
    }
  }

  @Override
  public LocalDateTime lastModifiedAt() {
    var result = virtualFile == null ? file.lastModified() : virtualFile.getModificationStamp();
    return Instant.ofEpochMilli(result).atZone(ZoneId.systemDefault()).toLocalDateTime();
  }

  @Override
  public boolean exists() {
    if (virtualFile == null) {
      return file.exists();
    }
    return virtualFile.exists();
  }

  @Override
  public URI toUri() {
    return Files.toUri(virtualFile == null ? file : virtualFile.toNioPath().toFile());
  }

  public File toFile() {
    return virtualFile == null ? file : virtualFile.toNioPath().toFile();
  }

  @Override
  public String toString() {
    return path();
  }
}
