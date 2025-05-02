package org.setms.sew.intellij.editor;

import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Pattern;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.InputSource;
import org.setms.sew.core.domain.model.tool.Tool;

public class VirtualFileInputSource implements InputSource {

  private final VirtualFile file;

  public VirtualFileInputSource(VirtualFile file) {
    this.file = file;
  }

  public VirtualFileInputSource(VirtualFile file, Tool tool) {
    this(rootOf(file, tool));
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
    Optional.ofNullable(file.findFileByRelativePath(glob.path()))
        .ifPresent(
            ancestor -> {
              var pattern = Pattern.compile(glob.pattern().replace("**/*.", ".+\\."));
              addChildren(ancestor, pattern, result);
            });
    return result;
  }

  @SuppressWarnings("UnsafeVfsRecursion")
  private void addChildren(
      VirtualFile file, Pattern pattern, Collection<VirtualFileInputSource> sources) {
    Optional.ofNullable(file.getChildren()).stream()
        .flatMap(Arrays::stream)
        .forEach(
            child -> {
              if (pattern.matcher(child.getName()).matches()) {
                sources.add(new VirtualFileInputSource(child));
              } else {
                addChildren(child, pattern, sources);
              }
            });
  }

  @Override
  public InputStream open() throws IOException {
    return file.getInputStream();
  }
}
