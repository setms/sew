package org.setms.km.domain.model.kmsystem;

import java.util.TreeMap;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

class InMemoryWorkspace extends Workspace {

  @Override
  protected Resource<?> newRoot() {
    return new InMemoryResource(new TreeMap<>(), "/", this::onChanged, this::onDeleted);
  }

  void onChanged(String path) {
    parse(path).ifPresent(artifact -> onChanged(path, artifact));
  }
}
