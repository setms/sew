package org.setms.swe.e2e;

import java.io.File;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(of = "file")
public class Input {

  String file;
  String location;
  String alias;

  public File resolve(File directory) {
    return directory.toPath().resolve("inputs").resolve(file).toFile();
  }
}
