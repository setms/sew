package org.setms.sew.core.inbound.tool;

import java.io.File;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.validation.Level;
import org.setms.km.outbound.workspace.file.DirectoryWorkspace;

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
      var tool = (BaseTool) toolType.getDeclaredConstructor().newInstance();
      var workspace = new DirectoryWorkspace(new File(args[1]), new File(args[2]));
      var diagnostics = tool.build(workspace);
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
