package org.setms.sew.intellij.editor;

import static java.util.stream.Collectors.joining;
import static org.setms.sew.core.domain.model.tool.Level.ERROR;

import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.outbound.tool.file.FileOutputSink;

public class HtmlPreview extends UserDataHolderBase implements FileEditor {

  private static final String STYLE =
      """
          document.body.style.backgroundColor = '%s';
          document.body.style.color = '%s';""";
  private static final String ERRORS =
      """
      <html>
        <body>
          <h1>Issues</h1>
          <ul>
            <li>%s</li>
          </ul>
        </body>
      </html>""";
  private static final String ERROR_SEPARATOR = "</li><li>";
  private final JBCefBrowser browser;
  private final JPanel panel;
  private final RateLimiter rateLimiter;
  private final VirtualFile file;
  private final Tool tool;
  private OutputSink sink;

  public HtmlPreview(Project ignored, VirtualFile file, @NotNull Tool tool) {
    this.tool = tool;
    this.file = file;
    panel = new JPanel(new BorderLayout());
    var document = FileDocumentManager.getInstance().getDocument(file);
    if (JBCefApp.isSupported() && document != null) {
      browser = new JBCefBrowser();
      panel.add(browser.getComponent(), BorderLayout.CENTER);
      rateLimiter = new RateLimiter(this::showDocument, 500);
      initBrowser(document);
    } else {
      browser = null;
      panel.add(new JLabel("In-place browser not supported."));
      rateLimiter = null;
    }
  }

  private void initBrowser(Document document) {
    setBrowserStyle();
    updateStyleWhenColorSchemeChanges();
    trackDocumentChanges(document);
    showDocument();
  }

  private void setBrowserStyle() {
    browser
        .getJBCefClient()
        .addLoadHandler(
            new CefLoadHandlerAdapter() {
              @Override
              public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                forceStyle();
              }
            },
            browser.getCefBrowser());
  }

  private void trackDocumentChanges(Document document) {
    document.addDocumentListener(
        new com.intellij.openapi.editor.event.DocumentListener() {
          @Override
          public void documentChanged(
              @NotNull com.intellij.openapi.editor.event.DocumentEvent event) {
            assert rateLimiter != null;
            rateLimiter.call();
          }
        });
  }

  private void updateStyleWhenColorSchemeChanges() {
    ApplicationManager.getApplication()
        .getMessageBus()
        .connect()
        .subscribe(LafManagerListener.TOPIC, (LafManagerListener) source -> forceStyle());
  }

  protected void forceStyle() {
    browser.getCefBrowser().executeJavaScript(getStyle(), browser.getCefBrowser().getURL(), 0);
  }

  private @NotNull String getStyle() {
    var scheme = EditorColorsManager.getInstance().getGlobalScheme();
    return STYLE.formatted(
        htmlColorOf(scheme.getDefaultBackground()), htmlColorOf(scheme.getDefaultForeground()));
  }

  private String htmlColorOf(Color color) {
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }

  protected void showDocument() {
    SwingUtilities.invokeLater(this::updateDocument);
  }

  private void updateDocument() {
    deleteSink();
    sink = new FileOutputSink();
    var diagnostics =
        tool.build(new VirtualFileInputSource(file, tool), sink).stream()
            .filter(diagnostic -> diagnostic.level() == ERROR)
            .toList();
    if (!diagnostics.isEmpty()) {
      browser.loadHTML(
          ERRORS.formatted(
              diagnostics.stream()
                  .map(diagnostic -> "%s: %s".formatted(diagnostic.level(), diagnostic.message()))
                  .collect(joining(ERROR_SEPARATOR))));
      return;
    }
    var content = sink.matching(new Glob("", "**/*.html"));
    if (content.isEmpty()) {
      browser.loadURL("about:blank");
    } else {
      browser.loadURL(content.getFirst().toUri().toString());
    }
  }

  private void deleteSink() {
    if (sink != null) {
      try {
        sink.delete();
      } catch (IOException ignored) {
        // Ignore: someone will clean up temp files at some point
      }
    }
    sink = null;
  }

  @Override
  public @NotNull JComponent getComponent() {
    return panel;
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return panel;
  }

  @Override
  public @NotNull String getName() {
    return "Preview";
  }

  @Override
  public void dispose() {
    if (rateLimiter != null) {
      rateLimiter.cancel();
    }
    if (browser != null) {
      browser.dispose();
    }
    deleteSink();
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}

  @Override
  public void setState(@NotNull FileEditorState state) {}
}
