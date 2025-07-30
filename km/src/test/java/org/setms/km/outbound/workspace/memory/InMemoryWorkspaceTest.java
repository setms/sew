package org.setms.km.outbound.workspace.memory;

import org.setms.km.domain.model.workspace.Workspace;
import org.setms.km.outbound.workspace.WorkspaceTestCase;

class InMemoryWorkspaceTest extends WorkspaceTestCase {

  @Override
  protected Workspace newWorkspace() {
    return new InMemoryWorkspace();
  }
}
