package org.setms.sew.intellij.tool;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

@RequiredArgsConstructor
public class VirtualFileWorkspace extends Workspace {

  private final VirtualFile file;
  private final Predicate<VirtualFile> inputFilter;

  public VirtualFileWorkspace(PsiFile file, BaseTool tool) {
    this(rootOf(file.getVirtualFile(), tool), f -> true);
  }

  public VirtualFileWorkspace(VirtualFile file, BaseTool tool) {
    this(rootOf(file, tool), f -> !extensionOf(f).equals(extensionOf(file)) || f.equals(file));
  }

  private static VirtualFile rootOf(VirtualFile file, BaseTool tool) {
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

  private static String extensionOf(VirtualFile file) {
    var name = file.getName();
    var index = name.lastIndexOf('.');
    return index < 0 ? "" : name.substring(index);
  }

  @Override
  protected Resource<?> newRoot() {
    return new VirtualFileResource(file, inputFilter);
  }
}
