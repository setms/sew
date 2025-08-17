package org.setms.swe.inbound.tool;

import static java.util.stream.Collectors.toSet;
import static org.setms.km.domain.model.format.Strings.wrap;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.Inputs.*;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.view.mxGraph;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.swing.SwingConstants;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.BaseDiagramTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.Tool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.architecture.Module;
import org.setms.swe.domain.model.sdlc.architecture.Modules;
import org.setms.swe.domain.model.sdlc.ddd.Domain;
import org.setms.swe.domain.model.sdlc.ddd.Subdomain;

public class DomainTool extends BaseDiagramTool<Domain> {

  private static final String VERTEX_STYLE = "shape=ellipse;fontColor=#6482B9;fillColor=none;";
  private static final int MAX_TEXT_LENGTH = 15;
  public static final String CREATE_MODULES = "modules.create";

  @Override
  public Input<Domain> getMainInput() {
    return domains();
  }

  @Override
  public Set<Input<?>> additionalInputs() {
    return Set.of(useCases(), modules());
  }

  @Override
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    inputs.get(Domain.class).forEach(domain -> build(domain, resource, diagnostics));
  }

  private void build(Domain domain, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var report = resource.select(domain.getName() + ".html");
    try (var writer = new PrintWriter(report.writeTo())) {
      writer.println("<html>");
      writer.println("  <body>");
      build(domain, toGraph(domain), resource, diagnostics)
          .ifPresent(
              image ->
                  writer.printf(
                      "    <img src=\"%s\" width=\"100%%\">%n",
                      report.toUri().resolve(".").normalize().relativize(image.toUri())));
      //      buildCoreDomainChart(domain, resource, diagnostics)
      //          .ifPresent(
      //              coreDomainChart ->
      //                  writer.printf(
      //                      "    <img src=\"%s\" width=\"100%%\">%n",
      //
      // report.toUri().resolve(".").normalize().relativize(coreDomainChart.toUri())));
      writer.println("  </body>");
      writer.println("</html>");
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private mxGraph toGraph(Domain domain) {
    var result = new mxGraph();
    result.getModel().beginUpdate();
    try {
      buildGraph(domain, result);
      layoutGraph(result);
    } finally {
      result.getModel().endUpdate();
    }
    return result;
  }

  private void buildGraph(Domain domain, mxGraph graph) {
    var verticesBySubdomain = new HashMap<Subdomain, Object>();
    domain
        .getSubdomains()
        .forEach(subdomain -> verticesBySubdomain.put(subdomain, addVertex(subdomain, graph)));
    domain
        .getSubdomains()
        .forEach(
            source ->
                source
                    .dependsOn()
                    .forEach(link -> addEdge(domain, source, link, verticesBySubdomain, graph)));
  }

  private Object addVertex(Subdomain domain, mxGraph graph) {
    return graph.insertVertex(
        graph.getDefaultParent(),
        null,
        wrap(domain.getName(), MAX_TEXT_LENGTH),
        0,
        0,
        120,
        60,
        VERTEX_STYLE);
  }

  private void addEdge(
      Domain domain,
      Subdomain source,
      Link link,
      Map<Subdomain, Object> verticesBySubdomain,
      mxGraph graph) {
    link.resolveFrom(domain.getSubdomains())
        .ifPresent(
            target -> {
              var from = verticesBySubdomain.get(source);
              var to = verticesBySubdomain.get(target);
              graph.insertEdge(graph.getDefaultParent(), null, "", from, to);
            });
  }

  private void layoutGraph(mxGraph graph) {
    var layout = new mxHierarchicalLayout(graph, SwingConstants.NORTH);
    layout.execute(graph.getDefaultParent());
  }

  @SuppressWarnings("unused") // I'll come back to this at some point
  private Optional<Resource<?>> buildCoreDomainChart(
      Domain domain, Resource<?> sink, Collection<Diagnostic> diagnostics) {
    return Optional.of(domain.getSubdomains())
        .filter(
            subdomains ->
                subdomains.stream().map(Subdomain::getClassification).allMatch(Objects::nonNull))
        .map(subdomains -> buildCoreDomainChart(domain.getName(), subdomains, sink, diagnostics));
  }

  private Resource<?> buildCoreDomainChart(
      String name,
      List<Subdomain> subdomains,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
    var dataset = new XYSeriesCollection();
    var core = new XYSeries("Core");
    var supporting = new XYSeries("Supporting");
    var generic = new XYSeries("Generic");

    var pointsBySubdomain = new HashMap<Subdomain, Point>();
    for (var subdomain : subdomains) {
      switch (subdomain.getClassification()) {
        case CORE -> place(subdomain, core, pointsBySubdomain);
        case SUPPORTING -> place(subdomain, supporting, pointsBySubdomain);
        case GENERIC -> place(subdomain, generic, pointsBySubdomain);
      }
    }

    dataset.addSeries(core);
    dataset.addSeries(supporting);
    dataset.addSeries(generic);

    var transparent = new Color(0, 0, 0, 0);

    var chart =
        ChartFactory.createScatterPlot(
            "Core Domain Chart", "Business Differentiation", "Model Complexity", dataset);
    chart.setBackgroundPaint(transparent);
    chart.setAntiAlias(true);

    var plot = (XYPlot) chart.getPlot();
    plot.setBackgroundPaint(transparent);
    plot.setDomainGridlinesVisible(false);
    plot.setRangeGridlinesVisible(false);
    var renderer = plot.getRenderer();
    renderer.setSeriesPaint(0, Color.RED);
    renderer.setSeriesPaint(1, Color.BLUE);
    renderer.setSeriesPaint(2, Color.GRAY);

    for (var subdomain : subdomains) {
      var point = pointsBySubdomain.get(subdomain);
      var annotation = new XYTextAnnotation(subdomain.getName(), point.x + 0.2, point.y);
      annotation.setFont(new Font("Arial", Font.PLAIN, 12));
      annotation.setTextAnchor(TextAnchor.HALF_ASCENT_LEFT);
      plot.addAnnotation(annotation);
    }

    var legend = chart.getLegend();
    legend.setPosition(RectangleEdge.BOTTOM);
    legend.setBackgroundPaint(transparent);
    legend.setFrame(new BlockBorder(transparent));

    var genericColor = new Color(160, 160, 160, 128);
    var supportingColor = new Color(180, 160, 220, 128);
    var coreColor = new Color(150, 220, 180, 128);

    // GENERIC: Column 0 (x = 0–2.5)
    plot.addAnnotation(new XYBoxAnnotation(0.0, 0.0, 2.5, 10.0, null, null, genericColor));

    // CORE: Top-right 2×2 cells (x = 5–10, y = 5–10)
    plot.addAnnotation(new XYBoxAnnotation(5.0, 5.0, 10.0, 10.0, null, null, coreColor));

    // SUPPORTING: Middle column (x = 2.5–5.0, y = 0–10)
    plot.addAnnotation(new XYBoxAnnotation(2.5, 0.0, 5.0, 10.0, null, null, supportingColor));

    // SUPPORTING: Bottom-right (x = 5–10, y = 0–5)
    plot.addAnnotation(new XYBoxAnnotation(5.0, 0.0, 10.0, 5.0, null, null, supportingColor));

    var width = 800;
    var height = 600;
    var result = resource.select("%s-core-domain-chart.png".formatted(name));
    try (var output = result.writeTo()) {
      ChartUtils.writeChartAsPNG(output, chart, width, height);
    } catch (Exception e) {
      addError(diagnostics, e.getMessage());
    }
    return result;
  }

  private void place(
      Subdomain subdomain, XYSeries series, Map<Subdomain, Point> pointsBySubdomain) {
    var aggregates = count(subdomain.getContent(), "aggregate");
    var commands = count(subdomain.getContent(), "command");
    var events = count(subdomain.getContent(), "event");
    var policies = count(subdomain.getContent(), "policy");
    var dependencies = subdomain.dependsOn().size();

    var x =
        switch (subdomain.getClassification()) {
          case CORE -> 7.0;
          case SUPPORTING -> 4.0;
          case GENERIC -> 2.0;
        };
    x += 0.3 * aggregates + 0.2 * commands;
    var y = 3.0 + 0.5 * events + 0.75 * policies + 0.3 * dependencies;

    var point = new Point((int) Math.min(x, 10), (int) Math.min(y, 10));

    series.add(point.x, point.y);
    pointsBySubdomain.put(subdomain, point);
  }

  private int count(Set<Link> content, String type) {
    return (int) content.stream().filter(p -> p.hasType(type)).count();
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var modules = inputs.get(Modules.class);
    inputs.get(Domain.class).forEach(domain -> validate(domain, modules, diagnostics));
  }

  private void validate(
      Domain domain, Collection<Modules> modules, Collection<Diagnostic> diagnostics) {
    if (modules.stream().noneMatch(containsModulesForAllSubdomainsIn(domain))) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Subdomains aren't mapped to modules",
              domain.toLocation(),
              List.of(new Suggestion(CREATE_MODULES, "Map to modules"))));
    }
  }

  private Predicate<? super Modules> containsModulesForAllSubdomainsIn(Domain domain) {
    return modules ->
        modules.getModules().stream()
            .map(Module::getMappedTo)
            .map(Link::getId)
            .collect(toSet())
            .equals(domain.getSubdomains().stream().map(Subdomain::getName).collect(toSet()));
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> domainResource,
      Domain domain,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs)
      throws IOException {
    if (CREATE_MODULES.equals(suggestionCode)) {
      return createModules(domainResource, domain);
    }
    return unknown(suggestionCode);
  }

  private AppliedSuggestion createModules(Resource<?> domainResource, Domain domain)
      throws IOException {
    var modules = mapDomainToModules(domain);
    var modulesResource = resourceFor(modules, domain, domainResource);
    try (var output = modulesResource.writeTo()) {
      builderFor(modules).build(modules, output);
      return created(modulesResource);
    }
  }

  private Modules mapDomainToModules(Domain domain) {
    var result = new Modules(getFullyQualifiedName(domain));
    result.setMappedTo(domain.linkTo());
    result.setModules(domain.getSubdomains().stream().map(this::subdmainToModule).toList());
    return result;
  }

  private FullyQualifiedName getFullyQualifiedName(Artifact object) {
    return new FullyQualifiedName("%s.%s".formatted(object.getPackage(), object.getName()));
  }

  private Module subdmainToModule(Subdomain subdomain) {
    var result = new Module(getFullyQualifiedName(subdomain));
    result.setMappedTo(subdomain.linkTo());
    return result;
  }
}
