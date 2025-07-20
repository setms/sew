package org.setms.km.domain.model.workspace;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class OutputSinkDecorator implements OutputSink {

  private final Workspace workspace;
  private final OutputSink decorated;

  @Override
  public OutputSink select(String path) {
    return new OutputSinkDecorator(workspace, decorated.select(path));
  }

  @Override
  public List<? extends OutputSink> matching(Glob glob) {
    return decorated.matching(glob).stream()
        .map(sink -> new OutputSinkDecorator(workspace, sink))
        .toList();
  }

  @Override
  public OutputStream open() throws IOException {
    return new OutputStreamDecorator(workspace, toUri(), decorated.open());
  }

  @Override
  public URI toUri() {
    return decorated.toUri();
  }

  @Override
  public List<? extends OutputSink> containers() {
    return decorated.containers().stream()
        .map(sink -> new OutputSinkDecorator(workspace, sink))
        .toList();
  }

  @Override
  public void delete() throws IOException {
    decorated.delete();
    workspace.onDeleted(toUri());
  }

  @Override
  public InputSource toInput() {
    return decorated.toInput();
  }
}
