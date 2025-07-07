package org.setms.sew.intellij.todo;

import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.intellij.filetype.BaseLanguageFileType;

public abstract class BaseChangeApplier implements AsyncFileListener.ChangeApplier {

  @NotNull protected final VirtualFile file;

  public BaseChangeApplier(@NotNull VirtualFile file) {
    this.file = file;
  }

  protected VirtualFile getFile() {
    return file;
  }

  protected Tool getTool() {
    return ((BaseLanguageFileType) file.getFileType()).getTool();
  }
}
