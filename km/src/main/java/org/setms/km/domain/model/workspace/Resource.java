package org.setms.km.domain.model.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

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

  List<T> matching(Glob glob);

  InputStream readFrom() throws IOException;

  OutputStream writeTo() throws IOException;

  void delete() throws IOException;
}
