package org.setms.sew.intellij.editor;

import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.setms.km.domain.model.kmsystem.KmSystem;
import org.setms.km.domain.model.workspace.Glob;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.sew.intellij.km.KmSystemService;
import org.setms.sew.intellij.workspace.IntellijWorkspace;

public class HtmlPreview extends UserDataHolderBase implements FileEditor {

  private static final String STYLE =
      """
          document.body.style.backgroundColor = '%s';
          document.body.style.color = '%s';""";
  private static final Glob HTML_GLOB = new Glob("", "**/*.html");

  private final JBCefBrowser browser;
  private final JPanel panel;
  private final RateLimiter rateLimiter;
  private final VirtualFile file;
  private final KmSystem kmSystem;
  private final IntellijWorkspace workspace;

  public HtmlPreview(Project project, VirtualFile file) {
    this.kmSystem = project.getService(KmSystemService.class).getKmSystem();
    this.workspace = (IntellijWorkspace) kmSystem.getWorkspace();
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

  private void updateStyleWhenColorSchemeChanges() {
    ApplicationManager.getApplication()
        .getMessageBus()
        .connect()
        .subscribe(LafManagerListener.TOPIC, (LafManagerListener) source -> forceStyle());
  }

  private void forceStyle() {
    browser.getCefBrowser().executeJavaScript(getStyle(), browser.getCefBrowser().getURL(), 0);
  }

  private String getStyle() {
    var scheme = EditorColorsManager.getInstance().getGlobalScheme();
    return STYLE.formatted(
        htmlColorOf(scheme.getDefaultBackground()), htmlColorOf(scheme.getDefaultForeground()));
  }

  private String htmlColorOf(Color color) {
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
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

  private void showDocument() {
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              updateFile();
              showFile();
            });
  }

  private void updateFile() {
    try {
      var fileContents = new ByteArrayOutputStream();
      file.getInputStream().transferTo(fileContents);
      var documentManager = FileDocumentManager.getInstance();
      var document = documentManager.getDocument(file);
      var text = document.getText();
      if (!text.equals(fileContents.toString())) {
        WriteAction.run(
            () -> {
              documentManager.saveDocument(document);
              workspace.changed(file);
            });
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to update virtual file from document", e);
    }
  }

  private void showFile() {
    browser.loadURL(
        Optional.ofNullable(workspace.find(file))
            .map(Resource::path)
            .map(kmSystem::mainReportFor)
            .map(report -> report.matching(HTML_GLOB))
            .map(List::getFirst)
            .map(Resource::toUri)
            .map(Object::toString)
            .orElse("about:blank"));
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
