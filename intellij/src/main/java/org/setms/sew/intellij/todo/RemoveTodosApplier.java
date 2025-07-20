package org.setms.sew.intellij.todo;

import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

public class RemoveTodosApplier extends BaseChangeApplier {

  public RemoveTodosApplier(@NotNull VirtualFile file) {
    super(file);
  }

  @Override
  public void beforeVfsChange() {
    var tool = getTool();
    if (tool == null) {
      return;
    }
    var toolInput = tool.getInputs().getFirst();
    try (var input = file.getInputStream()) {
      var artifact = toolInput.format().newParser().parse(input, toolInput.type(), false);
      var location = artifact.toLocation();
      System.out.printf("Removing todos about %s%n", location);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
