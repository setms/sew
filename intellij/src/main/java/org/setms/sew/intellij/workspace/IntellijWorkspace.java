package org.setms.sew.intellij.workspace;

import com.intellij.openapi.vfs.VirtualFile;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

@RequiredArgsConstructor
public class IntellijWorkspace extends Workspace<VirtualFile> {

  private final VirtualFile file;

  @Override
  protected Resource<?> newRoot() {
    return new VirtualFileResource(file, null);
  }

  @Override
  public Resource<?> find(VirtualFile external) {
    return new VirtualFileResource(external, null);
  }
}
