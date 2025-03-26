package org.setms.sew.format;

public interface Format {

  Parser newParser();

  Builder newBuilder();

}
