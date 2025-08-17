package org.setms.km.domain.model.diagram;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.outbound.workspace.dir.DirectoryWorkspace;

class BaseDiagramToolTest {

  @Test
  void shouldRenderHtmlWithImage() {
    var workspace = new DirectoryWorkspace(new File("build/diagram-test"));
    var tool = new FooDiagramTool();
    var diagnostics = new ArrayList<Diagnostic>();

    tool.build(null, workspace.root(), diagnostics);

    assertThat(diagnostics).as("Diagnostics").isEmpty();
    assertThat(workspace.root().children())
        .hasSize(2)
        .allSatisfy(
            child -> {
              assertThat(child.name()).as("Name").startsWith("Bear.");
              assertThat(child.name().substring(1 + child.name().lastIndexOf('.')))
                  .as("Extensions")
                  .isIn("html", "png");
            });
  }
}
