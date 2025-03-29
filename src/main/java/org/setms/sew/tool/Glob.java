package org.setms.sew.tool;

import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import lombok.Value;

@Value
@SuppressWarnings("ClassCanBeRecord")
public class Glob {

  String path;
  String pattern;

  public Collection<File> matchingIn(File dir) {
    try {
      var root = new File(dir, path).getCanonicalFile().toPath();
      var pathMatcher = root.getFileSystem().getPathMatcher("glob:" + pattern);
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

  @Override
  public String toString() {
    return "%s/%s".formatted(path, pattern);
  }
}
