package org.setms.swe.e2e;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface InputStreamProvider<T> {

  InputStream apply(T source) throws IOException;
}
