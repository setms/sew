package org.setms.sew.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public interface Parser {

  RootObject parse(InputStream input) throws IOException;

  default RootObject parse(File file) throws IOException {
    try (var input = new FileInputStream(file)) {
      return parse(input);
    }
  }

}
