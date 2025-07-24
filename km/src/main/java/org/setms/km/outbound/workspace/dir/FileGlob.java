package org.setms.km.outbound.workspace.dir;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.workspace.Glob;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class FileGlob {

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
