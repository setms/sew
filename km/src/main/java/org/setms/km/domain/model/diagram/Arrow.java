package org.setms.km.domain.model.diagram;

public record Arrow(Box from, Box to, String fromText, String toText, String middleText) {

  public Arrow(Box from, Box to, String middleText) {
    this(from, to, null, null, middleText);
  }
}
