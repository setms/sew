package org.setms.sew.intellij.todo;

import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.intellij.filetype.BaseLanguageFileType;

public class FileListener implements AsyncFileListener, BulkFileListener {

  @Override
  public @Nullable ChangeApplier prepareChange(
      @NotNull List<? extends @NotNull VFileEvent> events) {
    var result = new CompositeChangeApplier();
    events.stream().filter(this::isSewFile).flatMap(this::changeApplierFor).forEach(result::add);
    return result;
  }

  private boolean isSewFile(VFileEvent event) {
    return event.getFile() != null && event.getFile().getFileType() instanceof BaseLanguageFileType;
  }

  private Stream<ChangeApplier> changeApplierFor(@NotNull VFileEvent event) {
    return switch (event) {
      case VFileCreateEvent create -> Stream.of(new ValidateApplier(create.getFile()));
      case VFileContentChangeEvent change ->
          Stream.of(
              new RemoveTodosApplier(change.getFile()), new ValidateApplier(change.getFile()));
      case VFileDeleteEvent delete -> Stream.of(new RemoveTodosApplier(delete.getFile()));
      default -> Stream.empty();
    };
  }

  @Override
  public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
    events.stream()
        .filter(VFileCreateEvent.class::isInstance)
        .filter(this::isSewFile)
        .flatMap(this::changeApplierFor)
        .forEach(ChangeApplier::afterVfsChange);
  }
}
