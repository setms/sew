package org.setms.km.domain.model.format;

public interface Format {

  Parser newParser();

  Builder newBuilder();
}
