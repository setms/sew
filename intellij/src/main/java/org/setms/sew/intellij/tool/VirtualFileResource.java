package org.setms.sew.intellij.tool;

import static java.util.function.Predicate.not;

import com.intellij.openapi.vfs.VirtualFile;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;

@RequiredArgsConstructor
public class VirtualFileResource implements Resource<VirtualFileResource> {

  private final VirtualFile virtualFile;
  private final Predicate<VirtualFile> fileFilter;
  private final File file;

  @Override
  public String name() {
    return virtualFile == null ? file.getName() : virtualFile.getName();
  }

  @Override
  public String path() {
    return virtualFile == null ? file.getPath() : virtualFile.getPath();
  }

  @Override
  public Optional<VirtualFileResource> parent() {
    if (virtualFile == null) {
      return Optional.ofNullable(file.getParentFile())
          .map(parent -> new VirtualFileResource(null, fileFilter, parent));
    }
    return Optional.ofNullable(virtualFile.getParent())
        .map(parent -> new VirtualFileResource(parent, fileFilter, null));
  }

  @Override
  public List<VirtualFileResource> children() {
    if (virtualFile == null) {
      return Files.childrenOf(file)
          .map(child -> new VirtualFileResource(null, fileFilter, child))
          .toList();
    }
    return Stream.ofNullable(virtualFile.getChildren())
        .flatMap(Arrays::stream)
        .filter(fileFilter)
        .map(child -> new VirtualFileResource(child, fileFilter, null))
        .toList();
  }

  @Override
  public VirtualFileResource select(String path) {
    if (path.startsWith(File.separator)) {
      return new VirtualFileResource(
          virtualFile.getFileSystem().refreshAndFindFileByPath(path), fileFilter, null);
    }
    if (virtualFile == null) {
      return new VirtualFileResource(null, fileFilter, new File(file, path));
    }
    var result = virtualFile.findFileByRelativePath(path);
    if (result == null) {
      return new VirtualFileResource(
          null, fileFilter, new File(virtualFile.toNioPath().toFile(), path));
    }
    return new VirtualFileResource(result, fileFilter, null);
  }

  @Override
  public List<VirtualFileResource> matching(Glob glob) {
    if (virtualFile == null) {
      return Files.matching(file, glob).stream()
          .map(found -> new VirtualFileResource(null, fileFilter, found))
          .toList();
    }
    var result = new ArrayList<VirtualFileResource>();
    var ancestor =
        Optional.ofNullable(glob.path())
            .filter(not(String::isBlank))
            .map(virtualFile::findFileByRelativePath)
            .orElse(virtualFile);
    var pattern = Pattern.compile(glob.pattern().replace("**/*.", ".+\\."));
    addChildren(ancestor, pattern, result);
    return result;
  }

  @SuppressWarnings("UnsafeVfsRecursion")
  private void addChildren(
      VirtualFile file, Pattern pattern, Collection<VirtualFileResource> sources) {
    Optional.ofNullable(file.getChildren()).stream()
        .flatMap(Arrays::stream)
        .filter(fileFilter)
        .forEach(
            child -> {
              if (pattern.matcher(child.getName()).matches() && fileFilter.test(child)) {
                sources.add(new VirtualFileResource(child, fileFilter, null));
              } else {
                addChildren(child, pattern, sources);
              }
            });
  }

  @Override
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public InputStream readFrom() throws IOException {
    if (virtualFile == null) {
      file.getParentFile().mkdirs();
      return new FileInputStream(file);
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
    }
    virtualFile.delete(null);
  }

  @Override
  public URI toUri() {
    if (virtualFile == null) {
      return file.toURI();
    }
    return virtualFile.toNioPath().toUri();
  }
}
