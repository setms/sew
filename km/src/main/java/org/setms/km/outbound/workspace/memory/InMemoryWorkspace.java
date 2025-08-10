package org.setms.km.outbound.workspace.memory;

import java.util.TreeMap;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

public class InMemoryWorkspace extends Workspace<Void> {

  @Override
  protected Resource<?> newRoot() {
    return new InMemoryResource( "/", this::onChanged, this::onDeleted);
  }

  @Override
  public Resource<?> find(Void unsupported) {
    return null;
  }

  void onChanged(String path) {
    parse(path).ifPresent(artifact -> onChanged(path, artifact));
  }
}
