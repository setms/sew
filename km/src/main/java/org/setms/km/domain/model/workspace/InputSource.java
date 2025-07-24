package org.setms.km.domain.model.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

public interface InputSource {

  Collection<? extends InputSource> matching(Glob glob);

  String name();

  URI toUri();

  InputStream open() throws IOException;

  InputSource select(String path);
}
