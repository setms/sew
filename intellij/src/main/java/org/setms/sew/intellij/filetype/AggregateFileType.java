package org.setms.sew.intellij.filetype;

public class AggregateFileType extends SalLanguageFileType {

  public static final AggregateFileType INSTANCE = new AggregateFileType();

  private AggregateFileType() {
    super("Aggregate", SewIcons.AGGREGATE, null);
  }
}
