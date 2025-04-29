package org.setms.sew.intellij;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import org.setms.sew.core.tool.Glob;
import org.setms.sew.core.tool.InputSource;

public class StringInputSource implements InputSource {

  private final String text;

  public StringInputSource(String text) {
    this.text = text;
  }

  @Override
  public Collection<? extends InputSource> matching(Glob glob) {
    return List.of(this);
  }

  @Override
  public InputStream open() {
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }
}
