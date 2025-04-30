package org.setms.sew.intellij;

import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
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
import javax.swing.*;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.sew.core.domain.model.tool.FileOutputSink;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.tool.UseCaseTool;

public class HtmlPreviewFileEditor extends UserDataHolderBase implements FileEditor {

  public static final String STYLE =
      """
          document.body.style.backgroundColor = '%s';
          document.body.style.color = '%s';""";
  private final JBCefBrowser browser;
  private final JPanel panel;
  private final Debouncer debouncer;
  private final Tool tool = new UseCaseTool();
  private OutputSink sink;

  public HtmlPreviewFileEditor(Project ignored, VirtualFile file) {
    if (!JBCefApp.isSupported()) {
      panel = new JPanel();
      panel.add(new JLabel("In-place browser not supported."));
      browser = null;
      debouncer = null;
    } else {
      browser = new JBCefBrowser();
      panel = new JPanel(new BorderLayout());
      panel.add(browser.getComponent(), BorderLayout.CENTER);

      var client = browser.getJBCefClient();
      client.addDisplayHandler(
          new CefDisplayHandlerAdapter() {
            @Override
            public boolean onConsoleMessage(
                CefBrowser browser,
                CefSettings.LogSeverity level,
                String message,
                String source,
                int line) {
              System.out.println(message);
              return false;
            }
          },
          browser.getCefBrowser());
      client.addLoadHandler(
          new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
              forceStyle();
            }
          },
          browser.getCefBrowser());

      var document = FileDocumentManager.getInstance().getDocument(file);
      if (document != null) {
        debouncer = new Debouncer(() -> updateHtml(document), 500);
        document.addDocumentListener(
            new com.intellij.openapi.editor.event.DocumentListener() {
              @Override
              public void documentChanged(
                  @NotNull com.intellij.openapi.editor.event.DocumentEvent event) {
                debouncer.call();
              }
            });
        updateHtml(document);
      } else {
        debouncer = null;
        browser.loadHTML("<html><body><p>Could not load use case.</p></body></html>");
      }
    }

    LafManagerListener listener = source -> forceStyle();
    ApplicationManager.getApplication()
        .getMessageBus()
        .connect()
        .subscribe(LafManagerListener.TOPIC, listener);
  }

  private void forceStyle() {
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

  private void updateHtml(Document document) {
    SwingUtilities.invokeLater(() -> browser.loadURL(htmlUriOf(document.getText())));
  }

  private String htmlUriOf(String text) {
    if (sink != null) {
      try {
        sink.delete();
      } catch (IOException ignored) {
        // Ignore: someone will clean up temp files at some point
      }
    }
    sink = new FileOutputSink();
    tool.build(new StringInputSource(text), sink);
    return sink.matching(new Glob("reports/useCases", "**/*.html")).getFirst().toUri().toString();
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
    if (browser != null) {
      debouncer.cancel();
      browser.dispose();
    }
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
  public void selectNotify() {}

  @Override
  public void deselectNotify() {}

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {}

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {}

  @Override
  public @Nullable FileEditorLocation getCurrentLocation() {
    return null;
  }

  @Override
  public void setState(@NotNull FileEditorState state) {}
}
