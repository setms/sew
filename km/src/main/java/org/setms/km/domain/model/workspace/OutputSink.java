package org.setms.km.domain.model.workspace;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

public interface OutputSink {

  OutputSink select(String path);

  List<? extends OutputSink> matching(Glob glob);

  OutputStream open() throws IOException;

  URI toUri();

  List<? extends OutputSink> containers();

  void delete() throws IOException;

  InputSource toInput();
}
