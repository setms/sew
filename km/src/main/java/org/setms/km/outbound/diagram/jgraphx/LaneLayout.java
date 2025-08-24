package org.setms.km.outbound.diagram.jgraphx;

import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static org.setms.km.outbound.diagram.jgraphx.LanePatternType.SPLIT;
import static org.setms.km.outbound.diagram.jgraphx.LanePatternType.STRAIGHT;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LaneLayout extends mxGraphLayout {

  private List<mxCell> vertices;
  private List<mxCell> sources;
  private Collection<Lane> lanes;
  private Collection<LanePattern> patterns;

  public LaneLayout(mxGraph graph) {
    super(graph);
  }

  @Override
  public void execute(Object parent) {
    vertices = Arrays.stream(getGraph().getChildVertices(parent)).map(mxCell.class::cast).toList();
    if (vertices.size() <= 1) {
      return;
    }
    placeVertices();
  }

  private void placeVertices() {
    vertices.forEach(vertex -> place(vertex, 0, 0));
    sources = vertices.stream().filter(this::isSource).toList();
    lanes = collectLanes();
    patterns = findLanePatterns();
    placePatterns();
  }

  private boolean isSource(mxCell cell) {
    return getIncomingEdges(cell).isEmpty();
  }

  private List<mxCell> getIncomingEdges(mxCell cell) {
    return Arrays.stream(
            getGraph().getEdges(cell, getGraph().getDefaultParent(), true, false, false))
        .map(mxCell.class::cast)
        .toList();
  }

  private List<mxCell> getOutgoingEdges(mxCell cell) {
    return Arrays.stream(
            getGraph().getEdges(cell, getGraph().getDefaultParent(), false, true, false))
        .map(mxCell.class::cast)
        .toList();
  }

  private void place(mxCell cell, double x, double y) {
    var geo = cell.getGeometry();
    if (geo.getX() != x || geo.getY() != y) {
      geo.setX(x);
      geo.setY(y);
    }
  }

  private Collection<Lane> collectLanes() {
    return mergeConvergingPathsIntoLanes(collectPaths());
  }

  private Collection<List<mxCell>> collectPaths() {
    var result = new ArrayList<List<mxCell>>();
    sources.forEach(
        source ->
            getOutgoingEdges(source)
                .forEach(
                    edge -> {
                      var path = new ArrayList<mxCell>();
                      path.add(source);
                      var target = (mxCell) edge.getTarget();
                      path.add(target);
                      completePath(target, path);
                      result.add(path);
                    }));
    return result;
  }

  private void completePath(mxCell cell, Collection<mxCell> path) {
    getOutgoingEdges(cell)
        .forEach(
            edge -> {
              var target = (mxCell) edge.getTarget();
              path.add(target);
              completePath(target, path);
            });
  }

  private Collection<Lane> mergeConvergingPathsIntoLanes(Collection<List<mxCell>> paths) {
    var result = new ArrayList<Lane>();
    paths.forEach(
        path -> {
          var last = path.getLast();
          if (sources.contains(last)) {
            result.add(new Lane(path));
          } else {
            paths.stream()
                .filter(p -> p.getLast() == last && p.getFirst() != path.getFirst())
                .findFirst()
                .ifPresentOrElse(
                    otherPath -> {
                      var merged = new ArrayList<>(path);
                      otherPath.stream()
                          .filter(cell -> cell != last)
                          .forEach(cell -> merged.add(path.size(), cell));
                      if (isNewLane(merged, result)) {
                        result.add(new Lane(merged));
                      }
                    },
                    () -> {
                      if (isNewLane(path, result)) {
                        result.add(new Lane(path));
                      }
                    });
          }
        });
    return result;
  }

  private boolean isNewLane(Collection<mxCell> candidate, Collection<Lane> existing) {
    return existing.stream().map(Lane::toSet).noneMatch(lane -> lane.containsAll(candidate));
  }

  private Collection<LanePattern> findLanePatterns() {
    var result = new ArrayList<LanePattern>();
    var unused = new ArrayList<>(lanes);
    if (!addLanePatterns(unused, result)) {
      log.error("Didn't place all patterns");
    }
    return result;
  }

  private boolean addLanePatterns(List<Lane> lanes, Collection<LanePattern> patterns) {
    if (patterns.size() == 1) {
      patterns.add(new LanePattern(STRAIGHT, lanes));
      return true;
    }
    if (lanes.stream().map(Lane::getFirst).distinct().count() == 1L) {
      patterns.add(new LanePattern(SPLIT, lanes));
      return true;
    }
    if (lanes.size() == 2) {
      lanes.forEach(lane -> addLanePatterns(List.of(lane), patterns));
      return true;
    }
    log.error("Unsupported lane pattern: {}", lanes);
    lanes.forEach(lane -> addLanePatterns(List.of(lane), patterns));
    return false;
  }

  private void placePatterns() {
    var placed = new ArrayList<Lane>();
    var unplaced = new ArrayList<>(patterns);
    var pattern = biggestPatternIn(unplaced);
    do {
      placePattern(pattern);
      placed.addAll(pattern.lanes());
      unplaced.remove(pattern);
      if (!unplaced.isEmpty()) {
        pattern = findConnectedPattern(pattern);
        if (pattern == null) {
          pattern = biggestPatternIn(unplaced);
          startInNewLane(pattern, placed);
        }
      }
    } while (!unplaced.isEmpty());
  }

  private LanePattern biggestPatternIn(Collection<LanePattern> patterns) {
    return patterns.stream().max(comparing(LanePattern::size)).orElse(null);
  }

  private void placePattern(LanePattern pattern) {
    switch (pattern.type()) {
      case STRAIGHT -> place(pattern.lanes().getFirst());
      case SPLIT -> placeSplit(pattern.lanes());
      default -> log.error("Can't place pattern of type {}", pattern.type());
    }
  }

  private void placeSplit(List<Lane> lanes) {
    var bounds = lanes.getFirst().getFirst().getGeometry();
    var current = new AtomicReference<>(bounds.getY());
    lanes.forEach(
        lane -> {
          var geo = lane.getSecond().getGeometry();
          geo.setX(bounds.getX() + 2 * bounds.getWidth());
          geo.setY(current.getAndUpdate(y -> y + 2 * geo.getHeight()));
          place(lane.skip(1));
        });
  }

  private void place(Lane lane) {
    var midpoints = lane.stream().filter(not(this::isPlaced)).toList();
    var start = lane.getFirst().getGeometry();
    var delta = new mxPoint(2.0 * lane.width() / lane.size() - 1, 0);
    var current = new AtomicReference<mxPoint>(start);
    if (!midpoints.contains(lane.getFirst())) {
      step(current, delta);
    }
    midpoints.forEach(
        vertex -> {
          place(vertex, current.get().getX(), current.get().getY());
          step(current, delta);
        });
  }

  private boolean isPlaced(mxCell cell) {
    var geo = cell.getGeometry();
    return geo.getX() != 0 || geo.getY() != 0;
  }

  private void step(AtomicReference<mxPoint> current, mxPoint delta) {
    current.set(
        new mxPoint(current.get().getX() + delta.getX(), current.get().getY() + delta.getY()));
  }

  private mxPoint deltaBetween(mxPoint finish, mxPoint start, int numSegments) {
    return new mxPoint(
        (finish.getX() - start.getX()) / numSegments, (finish.getY() - start.getY()) / numSegments);
  }

  private LanePattern findConnectedPattern(LanePattern pattern) {
    // TODO: Implement
    return null;
  }

  private void startInNewLane(LanePattern pattern, Collection<Lane> placed) {
    var geo = pattern.lanes().getFirst().getFirst().getGeometry();
    geo.setY(geo.getHeight() + placed.stream().mapToDouble(Lane::maxY).max().orElse(0));
  }
}
