package org.setms.km.domain.model.workspace;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class OutputStreamDecorator extends OutputStream {

  private final Workspace workspace;
  private final URI artifactUri;
  private final OutputStream decorated;

  @Override
  public void write(int b) throws IOException {
    decorated.write(b);
  }

  @Override
  public void close() throws IOException {
    decorated.close();
    workspace.onChanged(artifactUri);
  }
}
