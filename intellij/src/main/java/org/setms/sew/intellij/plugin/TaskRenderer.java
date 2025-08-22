package org.setms.sew.intellij.plugin;

import com.intellij.ui.JBColor;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import org.setms.km.domain.model.validation.Diagnostic;

public class TaskRenderer extends JPanel implements ListCellRenderer<Diagnostic> {

  @Override
  public Component getListCellRendererComponent(
      JList<? extends Diagnostic> list,
      Diagnostic diagnostic,
      int index,
      boolean isSelected,
      boolean cellHasFocus) {
    removeAll();

    var message =
        diagnostic.hasSingleSuggestion()
            ? diagnostic.message()
            : diagnostic.suggestions().getFirst().message();
    var messageLabel = new JLabel(message);
    var locationLabel =
        new JLabel(Optional.ofNullable(diagnostic.location()).map(Objects::toString).orElse(""));
    locationLabel.setForeground(JBColor.GRAY);
    add(messageLabel, BorderLayout.WEST);
    add(locationLabel, BorderLayout.SOUTH);
    setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());

    return this;
  }
}
