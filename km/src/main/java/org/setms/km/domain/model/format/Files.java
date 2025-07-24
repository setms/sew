package org.setms.km.domain.model.format;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Files {

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static void delete(File file) {
    if (file.isDirectory()) {
      childrenOf(file).forEach(Files::delete);
    }
    file.delete();
  }

  private static Stream<File> childrenOf(File file) {
    return Stream.ofNullable(file.listFiles()).flatMap(Arrays::stream);
  }
}
