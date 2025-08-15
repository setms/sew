package org.setms.sew.intellij.plugin.filetype;

public class ReadModelFileType extends SalLanguageFileType {

  public static final ReadModelFileType INSTANCE = new ReadModelFileType();

  private ReadModelFileType() {
    super("ReadModel", "Read model", SewIcons.READ_MODEL);
  }
}
