package org.setms.sew.core.tool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface InputSource {

  Collection<? extends InputSource> matching(Glob glob);

  InputStream open() throws IOException;
}
