package org.setms.sew.format;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RootObject extends DataObject<RootObject> {

  private final String scope;
  private final String type;
  private final String name;

}
