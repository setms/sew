package org.setms.sew.intellij.todo;

import com.intellij.openapi.vfs.VirtualFile;
import java.util.Optional;
import org.setms.sew.intellij.tool.VirtualFileInputSource;

public class ValidateApplier extends BaseChangeApplier {

  public ValidateApplier(VirtualFile file) {
    super(file);
  }

  @Override
  public void afterVfsChange() {
    Optional.ofNullable(getTool())
        .ifPresent(tool -> tool.validate(new VirtualFileInputSource(getFile(), tool)));
  }
}
