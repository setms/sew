package org.setms.km.domain.model.diagram;

import static org.setms.km.domain.model.format.Strings.NL;
import static org.setms.km.domain.model.format.Strings.numLinesIn;
import static org.setms.km.domain.model.format.Strings.toFriendlyName;
import static org.setms.km.domain.model.format.Strings.wrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
@Accessors(chain = true)
public class Diagram {

  private static final int MAX_TEXT_LENGTH = 13;

  @Getter @Setter private Orientation orientation = Orientation.LEFT_TO_RIGHT;
  @Getter @Setter private Layout layout = Layout.DEFAULT;
  @Getter private final Collection<Box> boxes = new ArrayList<>();
  @Getter private final Collection<Arrow> arrows = new ArrayList<>();
  private final Map<String, Collection<Box>> boxesByType = new HashMap<>();

  public Box add(Box box) {
    return add(box, null);
  }

  public Box add(Box box, String reuseType) {
    var found =
        Optional.ofNullable(reuseType).map(boxesByType::get).stream()
            .flatMap(Collection::stream)
            .filter(candidate -> box.getText().equals(candidate.getText()))
            .findFirst();
    if (found.isPresent()) {
      return found.get();
    }
    boxes.add(box);
    if (reuseType != null) {
      boxesByType.computeIfAbsent(reuseType, ignored -> new ArrayList<>()).add(box);
    }
    return box;
  }

  public Arrow add(Arrow arrow) {
    arrows.add(arrow);
    return arrow;
  }

  public int normalizeTexts() {
    var maxLines = new AtomicInteger();
    boxes.forEach(
        box -> {
          box.setText(wrap(toFriendlyName(box.getText()), MAX_TEXT_LENGTH));
          var numLines = numLinesIn(box.getText());
          if (maxLines.get() < numLines) {
            maxLines.set(numLines);
          }
        });
    boxes.forEach(
        box -> {
          var text = box.getText();
          box.setText(text + NL.repeat(maxLines.get() - numLinesIn(text)));
        });
    return maxLines.get();
  }

  public Collection<Arrow> findArrowsTo(Box box) {
    return arrows.stream().filter(arrow -> arrow.to().equals(box)).toList();
  }
}
