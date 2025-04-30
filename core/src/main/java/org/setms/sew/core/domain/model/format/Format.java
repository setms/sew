package org.setms.sew.core.domain.model.format;

public interface Format {

  Parser newParser();

  Builder newBuilder();
}
