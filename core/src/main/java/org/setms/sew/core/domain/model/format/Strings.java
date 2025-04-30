package org.setms.sew.core.domain.model.format;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Strings {

  public static String initCap(String value) {
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }
}
