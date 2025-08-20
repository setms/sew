package org.setms.km.domain.model.diagram;

import static org.setms.km.domain.model.diagram.Placement.IN_MIDDLE;
import static org.setms.km.domain.model.diagram.Placement.NEAR_FROM_VERTEX;
import static org.setms.km.domain.model.diagram.Placement.NEAR_TO_VERTEX;

import java.util.EnumMap;

public record Arrow(Box from, Box to, EnumMap<Placement, String> texts, boolean bidirectional) {

  public Arrow(Box from, Box to, String middleText) {
    this(from, to, textsOf(middleText), false);
  }

  private static EnumMap<Placement, String> textsOf(String... texts) {
    var result = new EnumMap<Placement, String>(Placement.class);
    if (texts.length == 1) {
      result.put(IN_MIDDLE, texts[0]);
    } else if (texts.length == 2) {
      result.put(NEAR_FROM_VERTEX, texts[0]);
      result.put(NEAR_TO_VERTEX, texts[1]);
    }
    return result;
  }

  public Arrow(Box from, Box to, String fromText, String toText) {
    this(from, to, textsOf(fromText, toText), true);
  }

  public String middleText() {
    return texts.get(IN_MIDDLE);
  }

  public String fromText() {
    return texts.get(NEAR_FROM_VERTEX);
  }

  public String toText() {
    return texts.get(NEAR_TO_VERTEX);
  }
}
