package org.setms.sew.tool;

import lombok.Value;
import org.setms.sew.format.Format;
import org.setms.sew.schema.NamedObject;

@Value
@SuppressWarnings("ClassCanBeRecord")
public class Input<T extends NamedObject> {

  String name;
  Glob glob;
  Format format;
  Class<T> type;
}
