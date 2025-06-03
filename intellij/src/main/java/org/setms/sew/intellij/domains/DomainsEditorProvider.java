package org.setms.sew.intellij.domains;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.setms.sew.core.inbound.tool.DomainsTool;
import org.setms.sew.intellij.editor.EditorWithPreviewProvider;
import org.setms.sew.intellij.filetype.DomainsFileType;

public class DomainsEditorProvider extends EditorWithPreviewProvider {

  public DomainsEditorProvider() {
    super(new DomainsTool(), DomainsFileType.class);
  }

  @Override
  public @NotNull @NonNls String getEditorTypeId() {
    return "sew-domains-editor";
  }
}
