package org.setms.km.domain.model.kmsystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.setms.km.domain.model.format.Parser;
import org.setms.km.domain.model.format.RootObject;

class TestParser implements Parser {

  @Override
  public RootObject parse(InputStream input) throws IOException {
    try (var reader = new BufferedReader(new InputStreamReader(input))) {
      var lines = reader.lines().toList();
      var index = 0;
      var scope = lines.get(index++);
      var type = lines.get(index++);
      var name = lines.get(index++);
      return new RootObject(scope, type, name);
    }
  }
}
