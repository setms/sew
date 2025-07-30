package org.setms.km.outbound.workspace.dir;


import java.io.*;
import org.junit.jupiter.api.BeforeEach;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.km.outbound.workspace.WorkspaceTestCase;

class DirectoryWorkspaceTest extends WorkspaceTestCase {

  private final File file = new File("build/directory-workspace");
  private boolean changed;

  @BeforeEach
  protected void init() {
    Files.delete(file);
    super.init();
  }

  @Override
  protected Workspace newWorkspace() {
    return new DirectoryWorkspace(file);
  }

}
