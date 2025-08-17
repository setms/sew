package org.setms.km.outbound.diagram.jgraphx;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.diagram.Box;

@RequiredArgsConstructor
public class GraphContext {

  @Getter private final int boxHeight;
  private final Map<Box, Object> verticesByBox = new HashMap<>();

  public void associate(Box box, Object vertex) {
    verticesByBox.put(box, vertex);
  }

  public Object vertexFor(Box box) {
    return verticesByBox.get(box);
  }
}
