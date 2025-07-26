package org.setms.sew.intellij.editor;

import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.validation.Level.ERROR;

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
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Output;
import org.setms.km.domain.model.workspace.Workspace;
import org.setms.sew.intellij.tool.VirtualFileWorkspace;

public class HtmlPreview extends UserDataHolderBase implements FileEditor {

  private static final String STYLE =
      """
          document.body.style.backgroundColor = '%s';
          document.body.style.color = '%s';""";
  private static final String ERRORS =
      """
      <html>
        <body>
          <h1>Unable to show object</h1>
          <p>
            Please fix the following issues to show the object:
          </p>
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
  private final BaseTool tool;
  private Workspace workspace;

  public HtmlPreview(Project ignored, VirtualFile file, @NotNull BaseTool tool) {
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
    var glob = tool.getOutputs().map(Output::glob);
    if (glob.isEmpty()) {
      browser.loadURL("about:blank");
      return;
    }
    deleteOutput();
    workspace = new VirtualFileWorkspace(file, tool);
    var diagnostics =
        tool.build(workspace).stream().filter(diagnostic -> diagnostic.level() == ERROR).toList();
    if (!diagnostics.isEmpty()) {
      browser.loadHTML(
          ERRORS.formatted(
              diagnostics.stream()
                  .map(diagnostic -> "%s: %s".formatted(diagnostic.level(), diagnostic.message()))
                  .collect(joining(ERROR_SEPARATOR))));
      return;
    }
    var content = workspace.root().select("build").matching(glob.get());
    if (content.isEmpty()) {
      browser.loadURL("about:blank");
    } else {
      browser.loadURL(content.getFirst().toUri().toString());
    }
  }

  private void deleteOutput() {
    if (workspace != null) {
      try {
        workspace.root().select("build").delete();
      } catch (IOException ignored) {
        // Ignore: someone will clean up temp files at some point
      }
      workspace = null;
    }
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
    deleteOutput();
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
