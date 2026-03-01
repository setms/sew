package org.setms.km.domain.model.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public interface Resource<T extends Resource<T>> {

  String SEPARATOR = "/";

  String name();

  String path();

  URI toUri();

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

  OutputStream writeTo() throws IOException;

  void delete() throws IOException;

  LocalDateTime lastModifiedAt();

  /**
   * Check if this resource exists in the workspace.
   *
   * @return true if the resource exists, false otherwise
   */
  boolean exists();

  default void dump(boolean showAll) {
    show(root(), "", showAll);
  }

  private void show(Resource<?> resource, String indent, boolean showAll) {
    var children = resource.children().stream().filter(child -> isVisible(child, showAll)).toList();
    IntStream.range(0, children.size())
        .forEach(i -> showChild(children.get(i), indent, i == children.size() - 1, showAll));
  }

  default boolean isVisible(Resource<?> child, boolean showAll) {
    return !child.name().startsWith(".") && (showAll || !"build".equals(child.name()));
  }

  private void showChild(Resource<?> child, String indent, boolean isLast, boolean showAll) {
    System.out.println(indent + (isLast ? "└─ " : "├─ ") + child.name());
    Optional.of(child)
        .filter(c -> !c.children().isEmpty())
        .ifPresent(c -> show(c, indent + (isLast ? "   " : "│  "), showAll));
  }
}
