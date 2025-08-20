package org.setms.km.outbound.diagram.jgraphx;

import com.mxgraph.model.mxCell;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.diagram.Box;
import org.setms.km.domain.model.diagram.Placement;

@RequiredArgsConstructor
public class GraphContext {

  @Getter private final int boxHeight;
  private final Map<Box, Object> verticesByBox = new HashMap<>();
  @Getter private final Collection<EdgeLabel> edgeLabels = new ArrayList<>();

  public void associate(Box box, Object vertex) {
    verticesByBox.put(box, vertex);
  }

  public Object vertexFor(Box box) {
    return verticesByBox.get(box);
  }

  public void addEdgeLabel(Object edge, mxCell label, Placement placement) {
    edgeLabels.add(new EdgeLabel(edge, label, placement));
  }
}
