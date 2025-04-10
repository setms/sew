package org.setms.sew.core.format;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NestedObject extends DataObject<NestedObject> {

  private final String name;
}
