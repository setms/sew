package org.setms.sew.intellij.filetype;

public class ReadModelFileType extends SewLanguageFileType {

  public static final ReadModelFileType INSTANCE = new ReadModelFileType();

  private ReadModelFileType() {
    super("ReadModel", "Read model", SewIcons.READ_MODEL, null);
  }
}
