package org.setms.km.outbound.diagram.jgraphx;

import java.util.List;

public record LanePattern(LanePatternType type, List<Lane> lanes) {

  int size() {
    return lanes.size();
  }
}
