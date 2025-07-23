package org.setms.km.domain.model.workspace;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Workspace {

  private InputSource input;
  private OutputSink output;

  public InputSource input() {
    if (input == null) {
      input = newInputSource();
    }
    return input;
  }

  protected abstract InputSource newInputSource();

  public OutputSink output() {
    if (output == null) {
      output = new OutputSinkDecorator(this, newOutputSink());
    }
    return output;
  }

  protected abstract OutputSink newOutputSink();

  void onChanged(URI artifactUri) {
    log.info("Artifact at {} was changed", artifactUri);
  }

  void onDeleted(URI artifactUri) {
    log.info("Artifact at {} was deleted", artifactUri);
  }

  public void registerChangeHandler(ArtifactChangedHandler handler) {}
}
