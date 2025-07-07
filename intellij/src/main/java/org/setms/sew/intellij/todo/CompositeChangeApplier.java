package org.setms.sew.intellij.todo;

import com.intellij.openapi.vfs.AsyncFileListener;
import java.util.ArrayList;
import java.util.List;

public class CompositeChangeApplier implements AsyncFileListener.ChangeApplier {

  private final List<AsyncFileListener.ChangeApplier> appliers = new ArrayList<>();

  public void add(AsyncFileListener.ChangeApplier applier) {
    appliers.add(applier);
  }

  @Override
  public void beforeVfsChange() {
    appliers.forEach(AsyncFileListener.ChangeApplier::beforeVfsChange);
  }

  @Override
  public void afterVfsChange() {
    appliers.forEach(AsyncFileListener.ChangeApplier::afterVfsChange);
  }
}
