package org.setms.sew.core.domain.model.tool;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileGlob {

  public static List<File> matching(File file, Glob glob) {
    try {
      var root = new File(file, glob.path()).getCanonicalFile().toPath();
      var pathMatcher = root.getFileSystem().getPathMatcher("glob:" + glob.pattern());
      try (var paths = Files.walk(root)) {
        return paths
            .filter(Files::isRegularFile)
            .filter(Files::isReadable)
            .filter(pathMatcher::matches)
            .map(Path::toFile)
            .toList();
      }
    } catch (IOException e) {
      return emptyList();
    }
  }
}
