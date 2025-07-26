package org.setms.sew.intellij.tool;

import static java.util.function.Predicate.not;

import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;

@RequiredArgsConstructor
public class VirtualFileResource implements Resource<VirtualFileResource> {

  private final VirtualFile file;
  private final Predicate<VirtualFile> fileFilter;

  @Override
  public String name() {
    return file.getName();
  }

  @Override
  public Optional<VirtualFileResource> parent() {
    return Optional.ofNullable(file.getParent())
        .map(parent -> new VirtualFileResource(parent, fileFilter));
  }

  @Override
  public List<VirtualFileResource> children() {
    return Stream.ofNullable(file.getChildren())
        .flatMap(Arrays::stream)
        .filter(fileFilter)
        .map(child -> new VirtualFileResource(child, fileFilter))
        .toList();
  }

  @Override
  public Collection<VirtualFileResource> matching(Glob glob) {
    var result = new ArrayList<VirtualFileResource>();
    var ancestor =
        Optional.ofNullable(glob.path())
            .filter(not(String::isBlank))
            .map(file::findFileByRelativePath)
            .orElse(file);
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
                sources.add(new VirtualFileResource(child, fileFilter));
              } else {
                addChildren(child, pattern, sources);
              }
            });
  }

  @Override
  public InputStream readFrom() throws IOException {
    return file.getInputStream();
  }

  @Override
  public OutputStream writeTo() throws IOException {
    return file.getOutputStream(null);
  }

  @Override
  public void delete() throws IOException {
    file.delete(null);
  }
}
