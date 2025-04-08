package org.setms.sew.core.format;

public interface Format {

  Parser newParser();

  Builder newBuilder();
}
