package org.setms.swe.domain.model.sdlc.design;

public enum FieldType {
  TEXT,
  NUMBER,
  BOOLEAN,
  DATE,
  TIME,
  DATETIME,
  ID,
  SELECTION;

  public static FieldType parse(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing value");
    }
    return valueOf(value.toUpperCase());
  }
}
