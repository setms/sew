package org.setms.km.outbound.diagram.jgraphx;

import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.setms.km.outbound.diagram.jgraphx.LanePatternType.SPLIT;
import static org.setms.km.outbound.diagram.jgraphx.LanePatternType.STRAIGHT;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LaneLayout extends mxGraphLayout {

  private List<mxCell> vertices;
  private List<mxCell> sources;
  private Collection<Lane> lanes;
  private Collection<LanePattern> patterns;
  private Paths paths;

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
    vertices.forEach(vertex -> place(vertex, -1, -1));
    sources = vertices.stream().filter(this::isSource).toList();
    paths = collectPaths();
    lanes = combinePathsIntoLanes();
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

  private void place(mxCell cell, double x, double y) {
    var geo = cell.getGeometry();
    if (geo.getX() != x || geo.getY() != y) {
      geo.setX(x);
      geo.setY(y);
    }
  }

  private Paths collectPaths() {
    var result = new Paths();
    var unused = getEdges().collect(toList());
    var starting = unused.stream().filter(edge -> sources.contains(startOf(edge))).toList();
    starting.forEach(
        edge -> {
          result.add(toPath(edge));
          unused.remove(edge);
        });
    while (!unused.isEmpty()) {
      var used = new ArrayList<mxCell>();
      unused.forEach(
          edge -> {
            var start = startOf(edge);
            result.stream()
                .filter(path -> path.getLast() == start)
                .findFirst()
                .ifPresentOrElse(
                    path -> {
                      used.add(edge);
                      result.replace(path, path.join(toPath(edge)));
                    },
                    () -> {
                      used.add(edge);
                      result.add(toPath(edge));
                    });
          });
      if (used.isEmpty()) {
        unused.stream().map(this::toPath).forEach(result::add);
        unused.clear();
      } else {
        unused.removeAll(used);
      }
    }
    return result;
  }

  private mxCell startOf(mxCell edge) {
    return (mxCell) edge.getSource();
  }

  private Stream<mxCell> getEdges() {
    return Arrays.stream(getGraph().getChildEdges(getGraph().getDefaultParent()))
        .map(mxCell.class::cast);
  }

  private Path toPath(mxCell edge) {
    return new Path(startOf(edge), (mxCell) edge.getTarget());
  }

  private Collection<Lane> combinePathsIntoLanes() {
    var result = new ArrayList<Lane>();
    var unused = new Paths(paths);
    while (unused.hasItems()) {
      var partialLane = toLane(unused.getFirst(), unused);
      result.add(partialLane.toLane());
      unused.removeAll(partialLane.sources());
    }
    return result;
  }

  private PartialLane toLane(Path path, Paths paths) {
    var found =
        paths.stream()
            .filter(
                candidate ->
                    candidate != path
                        && candidate.getLast() == path.getLast()
                        && !path.contains(candidate.getFirst()))
            .findFirst();
    if (found.isEmpty()) {
      return new PartialLane(path, new Paths(path), false);
    }
    var connecting = found.get();
    return new PartialLane(
        path.join(connecting.reverse()), new Paths(Set.of(path, connecting)), true);
  }

  private Collection<LanePattern> findLanePatterns() {
    var result = new ArrayList<LanePattern>();
    var unused = new ArrayList<>(lanes);
    while (!unused.isEmpty()) {
      var patterns = findLanePatternsIn(unused);
      result.addAll(patterns);
      patterns.stream().map(LanePattern::lanes).forEach(unused::removeAll);
    }
    return result;
  }

  private Collection<LanePattern> findLanePatternsIn(List<Lane> lanes) {
    if (lanes.size() == 1) {
      return List.of(new LanePattern(STRAIGHT, new ArrayList<>(lanes)));
    }
    var first = lanes.getFirst().getFirst();
    var same = lanes.stream().filter(lane -> lane.getFirst() == first).toList();
    if (same.size() > 1) {
      return List.of(new LanePattern(SPLIT, sort(same)));
    }
    if (lanes.size() == 2) {
      return lanes.stream()
          .map(lane -> findLanePatternsIn(List.of(lane)))
          .flatMap(Collection::stream)
          .toList();
    }
    log.error("Unsupported lane pattern: {}", lanes);
    return lanes.stream()
        .map(lane -> findLanePatternsIn(List.of(lane)))
        .flatMap(Collection::stream)
        .toList();
  }

  private List<Lane> sort(List<Lane> lanes) {
    var result = new ArrayList<Lane>();
    var first = lanes.getFirst();
    result.add(first);
    var numConnectionsByLane = new HashMap<Lane, Integer>();
    lanes.stream()
        .skip(1)
        .forEach(lane -> numConnectionsByLane.put(lane, countConnectionsBetween(lane, first)));
    lanes.stream()
        .skip(1)
        .sorted((l1, l2) -> numConnectionsByLane.get(l2) - numConnectionsByLane.get(l1))
        .forEach(result::add);
    return result;
  }

  private int countConnectionsBetween(Lane lane, Lane base) {
    return (int) lane.stream().filter(base::contains).count();
  }

  private void placePatterns() {
    var cellDimensions =
        new mxPoint(
            3
                * vertices.stream()
                    .map(mxCell::getGeometry)
                    .mapToDouble(mxGeometry::getWidth)
                    .max()
                    .orElseThrow(),
            2
                * vertices.stream()
                    .map(mxCell::getGeometry)
                    .mapToDouble(mxGeometry::getHeight)
                    .max()
                    .orElseThrow());
    var rowTop = new AtomicReference<>(0.0);
    var unplaced = new ArrayList<>(patterns);
    var pattern = biggestPatternIn(unplaced);
    while (pattern != null) {
      placePattern(pattern, rowTop, cellDimensions);
      unplaced.remove(pattern);
      pattern = biggestPatternIn(unplaced);
    }
  }

  private LanePattern biggestPatternIn(Collection<LanePattern> patterns) {
    return patterns.stream().max(comparing(LanePattern::size)).orElse(null);
  }

  private void placePattern(
      LanePattern pattern, AtomicReference<Double> rowTop, mxPoint cellDimensions) {
    switch (pattern.type()) {
      case STRAIGHT -> place(pattern.lanes().getFirst(), rowTop, cellDimensions);
      case SPLIT -> placeSplit(pattern.lanes(), rowTop, cellDimensions);
      default -> log.error("Can't place pattern of type {}", pattern.type());
    }
  }

  private void placeSplit(
      List<Lane> lanes, AtomicReference<Double> rowTop, mxPoint cellDimensions) {
    lanes.forEach(lane -> place(lane, rowTop, cellDimensions));
  }

  private void place(Lane lane, AtomicReference<Double> rowTop, mxPoint cellDimensions) {
    var toPlacePoints = lane.stream().filter(not(this::isPlaced)).toList();
    if (toPlacePoints.isEmpty()) {
      return;
    }
    var anchor =
        Optional.ofNullable(lane.before(toPlacePoints.getFirst())).orElseGet(lane::getFirst);
    var start =
        new mxPoint(
            Math.max(anchor.getGeometry().getX(), 0),
            Math.max(
                anchor.getGeometry().getY(),
                rowTop.getAndAccumulate(cellDimensions.getY(), Double::sum)));
    var current = new AtomicReference<>(start);
    var delta = new mxPoint(cellDimensions.getX(), 0);
    if (!toPlacePoints.contains(anchor)) {
      step(current, delta);
    }
    toPlacePoints.forEach(
        vertex -> {
          place(vertex, current.get().getX(), current.get().getY());
          step(current, delta);
        });
  }

  private boolean isPlaced(mxCell cell) {
    var geo = cell.getGeometry();
    return geo.getX() >= 0 || geo.getY() >= 0;
  }

  private void step(AtomicReference<mxPoint> current, mxPoint delta) {
    current.set(
        new mxPoint(current.get().getX() + delta.getX(), current.get().getY() + delta.getY()));
  }
}
