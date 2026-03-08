package org.setms.swe.e2e;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;
import org.setms.km.domain.model.file.Files;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.km.domain.model.workspace.Workspace;

public class HtmlTranscript implements Consumer<String>, Closeable {

  private final File dir = new File("build/e2e-transcript");
  private final PrintWriter writer;
  private final Resource<?> root;
  private boolean inCode = false;
  private boolean alignRight;

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public HtmlTranscript(Workspace<?> workspace) {
    root = workspace.root();
    try {
      Files.delete(dir);
      dir.mkdirs();
      writer = new PrintWriter(new File(dir, "index.html"), UTF_8);
      writer.println(
          """
              <html>
                <head>
                  <title>End-to-end test</title>
                  <meta charset="UTF-8">
                  <style type='text/css'>
                    body {
                      width: 95%;
                    }
                    h1 {
                      text-align: center;
                    }
                    .right {
                      text-align: right;
                    }
                    .issue {
                      color: red;
                    }
                    .fix {
                      color: green;
                    }
                    .workspace {
                      font-size: smaller;
                      font-weight: bold;
                      text-align: left;
                    }
                    .code {
                      white-space: pre;
                      font-family: ui-monospace, "Courier New", monospace;
                    }
                  </style>
                </head>
                <body>
                  <div>""");
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void accept(String raw) {
    var line = inCode ? raw : raw.trim();
    if (line.contains("━━━")) {
      writer.printf("</div><hr/>%n<h1>%s</h1>%n<hr/><div>%n", line.replace("━", "").trim());
      if (line.contains("The End")) {
        writer.println("</div><div class='code'>");
        inCode = true;
      }
    } else if ("Human".equals(line)) {
      writer.printf("</div><h2>%s</h2><div>%n", line);
      alignRight = false;
    } else if ("SEW".equals(line)) {
      writer.printf("</div><h2 class='right'>%s</h2><div class='right'>", line);
      alignRight = true;
    } else if (line.contains("Workspace")) {
      writer.printf("</div><br/>%n<span class='workspace'>Workspace</span><pre>%n");
      inCode = true;
    } else if (inCode && raw.isBlank()) {
      writer.printf("</pre>%n<div%s>%n", alignRight ? " class='right'" : "");
      inCode = false;
    } else if (inCode) {
      writer.println(raw);
    } else {
      var item = line.replace(">", "").replace("<", "").trim();
      var start = item.indexOf("`");
      if (start > 0) {
        var end = item.lastIndexOf("`");
        item =
            "%s<span class='%s'>%s</span>%s"
                .formatted(
                    item.substring(0, start),
                    isIssue(item) ? "issue" : "fix",
                    item.substring(start + 1, end),
                    item.substring(end + 1));
      }
      writer.printf("%s", item);
      showImageFor(item);
      writer.println("<br/>");
    }
    writer.flush();
  }

  private boolean isIssue(String item) {
    return item.startsWith("Found issue");
  }

  private void showImageFor(String item) {
    if (item.startsWith("Created") || item.startsWith("Updated")) {
      var path = item.substring(item.indexOf(" ") + 1);
      root.select(".km/reports")
          .matching(path, "png")
          .forEach(
              image -> {
                var name =
                    "%s-%s.png"
                        .formatted(
                            image.name().substring(0, image.name().length() - 4), randomUUID());
                var copy = new File(dir, name);
                try (var output = new FileOutputStream(copy)) {
                  try (var input = image.readFrom()) {
                    input.transferTo(output);
                  }
                } catch (IOException e) {
                  throw new IllegalStateException(e);
                }
                writer.printf("<br/>%n<img src='%s'/>%n", name);
              });
    }
  }

  @Override
  public void close() {
    writer.println("</div></body></html>");
    writer.close();
  }
}
