package org.setms.km.domain.model.file;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.workspace.Glob;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Files {

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static void delete(File file) {
    if (file == null || !file.exists()) {
      return;
    }
    if (file.isDirectory()) {
      childrenOf(file).forEach(Files::delete);
    }
    file.delete();
  }

  public static Stream<File> childrenOf(File file) {
    return Stream.ofNullable(file.listFiles()).flatMap(Arrays::stream);
  }

  public static List<File> matching(File file, Glob glob) {
    try {
      var root = new File(file, glob.path()).getCanonicalFile().toPath();
      var pathMatcher = root.getFileSystem().getPathMatcher("glob:" + glob.pattern());
      try (var paths = java.nio.file.Files.walk(root)) {
        return paths
            .filter(java.nio.file.Files::isRegularFile)
            .filter(java.nio.file.Files::isReadable)
            .filter(pathMatcher::matches)
            .map(Path::toFile)
            .toList();
      }
    } catch (IOException e) {
      return emptyList();
    }
  }
}
