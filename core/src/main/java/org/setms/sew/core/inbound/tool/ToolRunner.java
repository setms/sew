package org.setms.sew.core.inbound.tool;

import java.io.File;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.domain.model.validation.Level;
import org.setms.sew.core.outbound.tool.file.FileInputSource;
import org.setms.sew.core.outbound.tool.file.FileOutputSink;

public class ToolRunner {

  public static void main(String... args) {
    if (args.length < 3) {
      System.err.println("Usage: <tool> <input> <output>");
      System.exit(1);
    }
    try {
      var toolType =
          ToolRunner.class
              .getClassLoader()
              .loadClass("%s.%s".formatted(ToolRunner.class.getPackageName(), args[0]));
      var tool = (Tool) toolType.getDeclaredConstructor().newInstance();
      var input = new FileInputSource(new File(args[1]));
      var output = new FileOutputSink(new File(args[2]));

      var diagnostics = tool.build(input, output);
      diagnostics.forEach(
          diagnostic -> {
            var stream = diagnostic.level() == Level.INFO ? System.out : System.err;
            stream.printf(
                "%s: %s%s%n",
                diagnostic.level(),
                diagnostic.message(),
                diagnostic.location() == null ? "" : diagnostic.location());
          });
    } catch (Exception e) {
      System.err.println(e.getMessage());
      System.exit(2);
    }
  }
}
