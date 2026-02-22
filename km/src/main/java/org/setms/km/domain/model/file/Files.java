package org.setms.km.domain.model.file;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.util.Collections.emptyList;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.setms.km.domain.model.workspace.Glob;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Files {

  private static final String FILE_URI_SCHEME = "file:";
  private static final String PATH_PREFIX = "//";

  public static void delete(File file) {
    if (file == null) {
      return;
    }
    var path = file.toPath();
    try {
      if (java.nio.file.Files.isDirectory(path, NOFOLLOW_LINKS)) {
        try (var children = java.nio.file.Files.newDirectoryStream(path)) {
          for (var child : children) {
            delete(child.toFile());
          }
        }
      }
      java.nio.file.Files.deleteIfExists(path);
    } catch (IOException e) {
      file.deleteOnExit();
      throw new UncheckedIOException(e);
    }
  }

  public static Stream<File> childrenOf(File file) {
    return Stream.ofNullable(file.listFiles()).flatMap(Arrays::stream);
  }

  public static List<File> matching(File file, Glob glob) {
    try {
      try (var paths = java.nio.file.Files.walk(file.toPath())) {
        return paths
            .filter(java.nio.file.Files::exists)
            .filter(java.nio.file.Files::isRegularFile)
            .filter(java.nio.file.Files::isReadable)
            .filter(path -> glob.matches(path.toString()))
            .map(Path::toFile)
            .toList();
      }
    } catch (IOException | UncheckedIOException e) {
      return emptyList();
    }
  }

  public static File get(String path) {
    if (path.startsWith(FILE_URI_SCHEME)) {
      path = path.substring(FILE_URI_SCHEME.length());
    }
    if (path.startsWith(PATH_PREFIX)) {
      path = path.substring(PATH_PREFIX.length());
    }
    return new File(path);
  }

  public static URI toUri(File file) {
    var result = file.toURI().toString();
    result = result.replaceFirst("^file:/([^/])", "file:///$1");
    return URI.create(result);
  }
}
