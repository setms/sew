package org.setms.swe.domain.model.sdlc.code;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.setms.km.domain.model.format.DataString;
import org.setms.km.domain.model.format.Parser;
import org.setms.km.domain.model.format.RootObject;

class CodeParser implements Parser {

  @Override
  public RootObject parse(InputStream input) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
      return new RootObject(null, null, null).set("code", new DataString(reader.readAllAsString()));
    }
  }
}
