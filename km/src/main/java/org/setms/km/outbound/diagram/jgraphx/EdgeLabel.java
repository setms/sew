package org.setms.km.outbound.diagram.jgraphx;

import com.mxgraph.model.mxCell;
import org.setms.km.domain.model.diagram.Placement;

record EdgeLabel(mxCell edge, mxCell label, Placement placement) {

  public boolean isPlacedNearFromVertex() {
    return placement == Placement.NEAR_FROM_VERTEX;
  }
}
