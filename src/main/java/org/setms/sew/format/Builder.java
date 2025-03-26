package org.setms.sew.format;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public interface Builder {

  default void build(RootObject root, File file) throws IOException {
    try (var writer = new PrintWriter(file)) {
      build(root, writer);
    }
  }

  void build(RootObject root, PrintWriter writer) throws IOException;

  default void build(RootObject root, OutputStream output) throws IOException {
    try (var writer = new PrintWriter(output)) {
      build(root, writer);
    }
  }
}
