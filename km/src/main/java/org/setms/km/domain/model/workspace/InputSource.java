package org.setms.km.domain.model.workspace;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface InputSource {

  Collection<? extends InputSource> matching(Glob glob);

  String name();

  InputStream open() throws IOException;

  OutputSink toSink();
}
