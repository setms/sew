package org.setms.sew.intellij.tool;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.InputSource;

@RequiredArgsConstructor
class VirtualFileInputSource implements InputSource {

  private final VirtualFile file;
  private final Predicate<VirtualFile> fileFilter;

  @Override
  public Collection<? extends InputSource> matching(Glob glob) {
    var result = new ArrayList<VirtualFileInputSource>();
    var ancestor =
        Optional.ofNullable(glob.path())
            .filter(not(String::isBlank))
            .map(file::findFileByRelativePath)
            .orElse(file);
    var pattern = Pattern.compile(glob.pattern().replace("**/*.", ".+\\."));
    addChildren(ancestor, pattern, result);
    return result;
  }

  @Override
  public String name() {
    return file.getName();
  }

  @Override
  public URI toUri() {
    return file.toNioPath().toUri();
  }

  @SuppressWarnings("UnsafeVfsRecursion")
  private void addChildren(
      VirtualFile file, Pattern pattern, Collection<VirtualFileInputSource> sources) {
    Optional.ofNullable(file.getChildren()).stream()
        .flatMap(Arrays::stream)
        .filter(fileFilter)
        .forEach(
            child -> {
              if (pattern.matcher(child.getName()).matches() && fileFilter.test(child)) {
                sources.add(new VirtualFileInputSource(child, fileFilter));
              } else {
                addChildren(child, pattern, sources);
              }
            });
  }

  @Override
  public InputStream open() throws IOException {
    var document = FileDocumentManager.getInstance().getDocument(file);
    if (document != null) {
      return new ByteArrayInputStream(document.getText().getBytes(UTF_8));
    }
    return file.getInputStream();
  }

  @Override
  public InputSource select(String path) {
    return new VirtualFileInputSource(
        path.startsWith(File.separator)
            ? file.getFileSystem().findFileByPath(path)
            : file.findFileByRelativePath(path),
        fileFilter);
  }
}
