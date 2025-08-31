package org.setms.swe.e2e;

import java.io.File;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Iteration {

  private File directory;
  private List<Input> inputs;
  private List<String> diagnostics;
  private List<String> outputs;

  @Override
  public String toString() {
    return Optional.ofNullable(directory).map(File::getName).orElse("???");
  }
}
