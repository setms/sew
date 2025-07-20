package org.setms.sew.intellij.tool;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.setms.km.domain.model.tool.Tool;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.InputSource;
import org.setms.km.domain.model.workspace.OutputSink;
import org.setms.km.outbound.workspace.file.FileOutputSink;

public class VirtualFileInputSource implements InputSource {

  private final VirtualFile file;
  private final Map<VirtualFile, Document> documentsByFile;
  private final Predicate<VirtualFile> fileFilter;

  public VirtualFileInputSource(
      VirtualFile file,
      Map<VirtualFile, Document> documentsByFile,
      Predicate<VirtualFile> fileFilter) {
    this.file = file;
    this.documentsByFile = documentsByFile;
    this.fileFilter = fileFilter;
  }

  public VirtualFileInputSource(VirtualFile file, Tool tool) {
    this(
        rootOf(file, tool),
        Collections.emptyMap(),
        f -> !extensionOf(f).equals(extensionOf(file)) || f.equals(file));
  }

  private static String extensionOf(VirtualFile file) {
    var name = file.getName();
    var index = name.lastIndexOf('.');
    return index < 0 ? "" : name.substring(index);
  }

  public VirtualFileInputSource(PsiFile file, Tool tool) {
    this(
        rootOf(file.getVirtualFile(), tool),
        Map.of(file.getVirtualFile(), toDocument(file)),
        f -> true);
  }

  private static Document toDocument(PsiFile file) {
    var result = FileDocumentManager.getInstance().getDocument(file.getVirtualFile());
    assert result != null;
    return result;
  }

  private static VirtualFile rootOf(VirtualFile file, Tool tool) {
    var path = file.getPath();
    var filePath = tool.getInputs().getFirst().glob().path();
    var index = path.indexOf(filePath);
    if (index < 0) {
      return file;
    }
    var numUp = path.substring(index).split("/").length;
    var result = file;
    for (var i = 0; i < numUp; i++) {
      result = result.getParent();
    }
    return result;
  }

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

  @SuppressWarnings("UnsafeVfsRecursion")
  private void addChildren(
      VirtualFile file, Pattern pattern, Collection<VirtualFileInputSource> sources) {
    Optional.ofNullable(file.getChildren()).stream()
        .flatMap(Arrays::stream)
        .filter(fileFilter)
        .forEach(
            child -> {
              if (pattern.matcher(child.getName()).matches()) {
                sources.add(new VirtualFileInputSource(child, documentsByFile, fileFilter));
              } else {
                addChildren(child, pattern, sources);
              }
            });
  }

  @Override
  public InputStream open() throws IOException {
    var document = documentsByFile.get(file);
    if (document != null) {
      return new ByteArrayInputStream(document.getText().getBytes(UTF_8));
    }
    return file.getInputStream();
  }

  @Override
  public OutputSink toSink() {
    return new FileOutputSink(new File(file.getPath()));
  }
}
