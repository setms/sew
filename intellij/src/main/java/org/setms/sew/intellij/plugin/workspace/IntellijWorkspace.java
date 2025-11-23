package org.setms.sew.intellij.plugin.workspace;

import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

@RequiredArgsConstructor
public class IntellijWorkspace extends Workspace<VirtualFile> {

  @Getter private final VirtualFile root;

  @Override
  protected Resource<?> newRoot() {
    return new VirtualFileResource(root, null, toPath(root));
  }

  private String toPath(VirtualFile file) {
    return file.toNioPath().toString();
  }

  @Override
  public Resource<?> find(VirtualFile external) {
    if (external == null) {
      return null;
    }
    var rootPath = toPath(root);
    if (!toPath(external).startsWith(rootPath)) {
      return null;
    }
    return new VirtualFileResource(external, null, rootPath);
  }

  public void changed(VirtualFile file) {
    var resource = find(file);
    if (resource == null) {
      return;
    }
    parse(resource.path()).ifPresent(artifact -> onChanged(resource.path(), artifact));
  }

  public void deleted(Resource<?> resource) {
    onDeleted(resource.path());
  }

  public File toFile(Resource<?> resource) {
    return resource instanceof VirtualFileResource virtualFileResource
        ? virtualFileResource.toFile()
        : null;
  }
}
