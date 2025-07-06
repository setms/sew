package org.setms.sew.intellij.filetype;

public class AggregateFileType extends SewLanguageFileType {

  public static final AggregateFileType INSTANCE = new AggregateFileType();

  private AggregateFileType() {
    super("Aggregate", SewIcons.AGGREGATE, null);
  }
}
