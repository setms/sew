package org.setms.sew.core.domain.model.tool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface InputSource {

  Collection<? extends InputSource> matching(Glob glob);

  String name();

  InputStream open() throws IOException;

  OutputSink toSink();
}
