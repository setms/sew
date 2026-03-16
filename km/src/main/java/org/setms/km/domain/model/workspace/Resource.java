package org.setms.km.domain.model.workspace;

import static org.setms.km.domain.model.file.Files.readAllAsString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public interface Resource<T extends Resource<T>> {

  String SEPARATOR = "/";

  String name();

  String path();

  URI toUri();

  default File toFile() {
    return new File(toUri());
  }

  Optional<T> parent();

  @SuppressWarnings("unchecked")
  default T root() {
    return parent().map(Resource::root).orElse((T) this);
  }

  List<T> children();

  T select(String path);

  // TODO: Replace path+extension by Glob, since that's what all implementations use
  List<T> matching(String path, String extension);

  InputStream readFrom() throws IOException;

  default String readAsString() {
    try (var input = readFrom()) {
      return readAllAsString(input);
    } catch (IOException e) {
      return "";
    }
  }

  OutputStream writeTo() throws IOException;

  default void writeAsString(String content) throws IOException {
    try (var writer = new OutputStreamWriter(writeTo())) {
      writer.write(content);
    }
  }

  default void delete() throws IOException {
    if (exists()) {
      doDelete();
    }
  }

  void doDelete() throws IOException;

  LocalDateTime createdAt();

  LocalDateTime lastModifiedAt();

  /**
   * Check if this resource exists in the workspace.
   *
   * @return true if the resource exists, false otherwise
   */
  boolean exists();

  @SuppressWarnings("unused")
  default void dump(boolean showAll) {
    dump(showAll, System.out::println);
  }

  default void dump(boolean showAll, Consumer<String> writer) {
    show(root(), "", showAll, writer);
  }

  private void show(Resource<?> resource, String indent, boolean showAll, Consumer<String> writer) {
    var children = resource.children().stream().filter(child -> isVisible(child, showAll)).toList();
    IntStream.range(0, children.size())
        .forEach(
            i -> showChild(children.get(i), indent, i == children.size() - 1, showAll, writer));
  }

  default boolean isVisible(Resource<?> child, boolean showAll) {
    return !child.name().startsWith(".") && (showAll || !"build".equals(child.name()));
  }

  private void showChild(
      Resource<?> child, String indent, boolean isLast, boolean showAll, Consumer<String> writer) {
    writer.accept(indent + (isLast ? "└─ " : "├─ ") + child.name());
    Optional.of(child)
        .filter(c -> !c.children().isEmpty())
        .ifPresent(c -> show(c, indent + (isLast ? "   " : "│  "), showAll, writer));
  }
}
