package org.setms.swe.inbound.tool;

import static org.setms.km.domain.model.format.Strings.toKebabCase;
import static org.setms.km.domain.model.tool.AppliedSuggestion.created;
import static org.setms.km.domain.model.tool.AppliedSuggestion.failedWith;
import static org.setms.km.domain.model.tool.AppliedSuggestion.unknown;
import static org.setms.km.domain.model.tool.Tools.builderFor;
import static org.setms.km.domain.model.validation.Level.WARN;
import static org.setms.swe.inbound.tool.CodeWriter.writeCodeArtifact;
import static org.setms.swe.inbound.tool.Inputs.decisions;
import static org.setms.swe.inbound.tool.Inputs.designSystems;
import static org.setms.swe.inbound.tool.Inputs.uiCode;
import static org.setms.swe.inbound.tool.Inputs.wireframes;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.tool.AppliedSuggestion;
import org.setms.km.domain.model.tool.ArtifactTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;
import org.setms.km.domain.model.validation.Suggestion;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.technology.TechnologyResolver;
import org.setms.swe.domain.model.sdlc.ui.DesignSystem;
import org.setms.swe.domain.model.sdlc.ui.Properties;
import org.setms.swe.domain.model.sdlc.ui.Property;
import org.setms.swe.domain.model.sdlc.ui.Style;
import org.setms.swe.domain.model.sdlc.ux.Affordance;
import org.setms.swe.domain.model.sdlc.ux.Container;
import org.setms.swe.domain.model.sdlc.ux.InputField;
import org.setms.swe.domain.model.sdlc.ux.Wireframe;
import org.setms.swe.domain.model.sdlc.ux.WireframeElement;

public class WireframeTool extends ArtifactTool<Wireframe> {

  static final String CREATE_DESIGN_SYSTEM = "designSystem.create";
  static final String CREATE_UI_CODE = "uiCode.create";

  private final TechnologyResolver resolver;

  public WireframeTool() {
    this(new TechnologyResolverImpl());
  }

  WireframeTool(TechnologyResolver resolver) {
    this.resolver = resolver;
  }

  private static final int SCREEN_WIDTH = 300;
  private static final int PADDING = 24;
  private static final int TITLE_H = 56;
  private static final int LABEL_H = 14;
  private static final int LABEL_GAP = 6;
  private static final int INPUT_H = 30;
  private static final int BTN_H = 40;
  private static final int ELEMENT_GAP = 20;
  private static final Color PAPER = new Color(0xFF, 0xFE, 0xF5);
  private static final Color INK = new Color(0x22, 0x22, 0x22);
  private static final Color LIGHT_INK = new Color(0x88, 0x88, 0x88);
  private static final Color FIELD_TINT = new Color(0xF2, 0xF2, 0xF2);
  private static final Color BTN_TINT = new Color(0xE8, 0xE8, 0xE8);
  private static final Font TITLE_FONT = new Font("Monospaced", Font.BOLD, 15);
  private static final Font LABEL_FONT = new Font("Monospaced", Font.PLAIN, 10);
  private static final Font BTN_FONT = new Font("Monospaced", Font.BOLD, 12);
  private static final Stroke SKETCH_STROKE =
      new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

  @Override
  public Set<Input<? extends Wireframe>> validationTargets() {
    return Set.of(wireframes());
  }

  @Override
  public Set<Input<? extends Artifact>> validationContext() {
    var result = new HashSet<Input<? extends Artifact>>(uiCode());
    result.add(designSystems());
    result.add(decisions());
    return result;
  }

  @Override
  public void validate(
      Wireframe wireframe, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (inputs.get(DesignSystem.class).isEmpty()) {
      diagnostics.add(
          new Diagnostic(
              WARN,
              "Missing design system",
              wireframe.toLocation(),
              new Suggestion(CREATE_DESIGN_SYSTEM, "Create design system")));
    } else {
      validateUiCode(wireframe, inputs, diagnostics);
    }
  }

  private void validateUiCode(
      Wireframe wireframe, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    if (hasMatchingUiCode(wireframe, inputs)) {
      return;
    }
    resolver
        .uiGenerator(inputs, diagnostics)
        .ifPresent(
            generator ->
                diagnostics.add(
                    new Diagnostic(
                        WARN,
                        "Missing UI code",
                        wireframe.toLocation(),
                        new Suggestion(CREATE_UI_CODE, "Create UI code"))));
  }

  private boolean hasMatchingUiCode(Wireframe wireframe, ResolvedInputs inputs) {
    return inputs.get(CodeArtifact.class).stream()
        .anyMatch(ca -> ca.getName().equals(wireframe.getName()));
  }

  @Override
  protected AppliedSuggestion doApply(
      Resource<?> resource,
      Wireframe wireframe,
      String suggestionCode,
      Location location,
      ResolvedInputs inputs)
      throws Exception {
    return switch (suggestionCode) {
      case CREATE_DESIGN_SYSTEM -> createDesignSystemFor(resource, wireframe);
      case CREATE_UI_CODE -> createUiCodeFor(resource, wireframe, inputs);
      default -> unknown(suggestionCode);
    };
  }

  private AppliedSuggestion createUiCodeFor(
      Resource<?> resource, Wireframe wireframe, ResolvedInputs inputs) {
    var diagnostics = new ArrayList<Diagnostic>();
    return inputs.get(DesignSystem.class).stream()
        .findFirst()
        .flatMap(
            ds -> resolver.uiGenerator(inputs, diagnostics).map(gen -> gen.generate(wireframe, ds)))
        .map(artifacts -> writeFrontendCode(artifacts, resource))
        .orElseGet(AppliedSuggestion::none);
  }

  private AppliedSuggestion writeFrontendCode(List<CodeArtifact> artifacts, Resource<?> resource) {
    var index = 0;
    var result = AppliedSuggestion.none();
    if (artifacts.size() > index) {
      var html = artifacts.get(index++);
      var htmlResource =
          resource.select(
              "/src/main/resources/static/%s.html".formatted(toKebabCase(html.getName())));
      writeCodeArtifact(html, htmlResource);
      result = AppliedSuggestion.created(htmlResource);
    }
    if (artifacts.size() > index) {
      var css = artifacts.get(index);
      var cssResource =
          resource.select(
              "/src/main/resources/static/css/%s.css".formatted(toKebabCase(css.getName())));
      writeCodeArtifact(css, cssResource);
      result = result.with(cssResource);
    }
    return result;
  }

  private AppliedSuggestion createDesignSystemFor(Resource<?> resource, Wireframe wireframe) {
    var designSystem = toDesignSystem(wireframe);
    var designSystemResource = resourceFor(designSystem, wireframe, resource);
    try {
      builderFor(designSystem).build(designSystem, designSystemResource);
    } catch (IOException e) {
      return failedWith(e);
    }
    return created(designSystemResource);
  }

  private DesignSystem toDesignSystem(Wireframe wireframe) {
    var pkg = wireframe.getPackage();
    var fqn = new FullyQualifiedName(pkg + ".Default");
    var properties =
        Properties.names().stream()
            .map(
                name ->
                    new Property(new FullyQualifiedName(pkg + "." + name))
                        .setValue(Properties.defaultFor(name)))
            .toList();
    var style = new Style(fqn).setProperties(properties);
    return new DesignSystem(fqn).setStyles(List.of(style));
  }

  @Override
  protected Input<? extends Artifact> reportingTargetInput() {
    return wireframes();
  }

  @Override
  public void buildReportsFor(
      Wireframe wireframe,
      ResolvedInputs inputs,
      Resource<?> resource,
      Collection<Diagnostic> diagnostics) {
    var parent = resource.select(wireframe.getName());
    try {
      var png = parent.select(wireframe.getName() + ".png");
      try (var out = png.writeTo()) {
        ImageIO.write(toImage(wireframe), "PNG", out);
      }
      writeHtml(wireframe, parent);
    } catch (IOException e) {
      addError(diagnostics, e.getMessage());
    }
  }

  private void writeHtml(Wireframe wireframe, Resource<?> parent) throws IOException {
    var html = parent.select(wireframe.getName() + ".html");
    try (var writer = new PrintWriter(html.writeTo())) {
      writer.println("<html>");
      writer.println("  <body>");
      writer.printf("    <h1>%s</h1>%n", wireframe.friendlyName());
      writer.printf("    <img src=\"%s.png\" width=\"100%%\">%n", wireframe.getName());
      writer.println("  </body>");
      writer.println("</html>");
    }
  }

  BufferedImage toImage(Wireframe wireframe) {
    var elements = collectElements(wireframe);
    var contentH =
        elements.stream().mapToInt(this::elementHeight).sum()
            + (elements.size() > 1 ? (elements.size() - 1) * ELEMENT_GAP : 0);
    var height = PADDING + TITLE_H + ELEMENT_GAP + contentH + PADDING;
    var image = new BufferedImage(SCREEN_WIDTH, height, BufferedImage.TYPE_INT_RGB);
    var g = (Graphics2D) image.getGraphics();
    render(wireframe, elements, g, height);
    g.dispose();
    return image;
  }

  private List<WireframeElement> collectElements(Wireframe wireframe) {
    var result = new ArrayList<WireframeElement>();
    Optional.ofNullable(wireframe.getContainers())
        .ifPresent(cs -> cs.forEach(c -> collectContainerElements(c, result)));
    return result;
  }

  private void collectContainerElements(Container container, List<WireframeElement> result) {
    Optional.ofNullable(container.getChildren())
        .ifPresent(children -> children.forEach(child -> collectElement(child, result)));
  }

  private void collectElement(WireframeElement element, List<WireframeElement> result) {
    if (element instanceof Container container) {
      collectContainerElements(container, result);
    } else {
      result.add(element);
    }
  }

  private int elementHeight(WireframeElement element) {
    if (element instanceof Affordance affordance) {
      return affordanceHeight(affordance);
    }
    return BTN_H;
  }

  private int affordanceHeight(Affordance affordance) {
    var inputFieldsH =
        Optional.ofNullable(affordance.getInputFields())
            .map(fields -> fields.size() * (LABEL_H + LABEL_GAP + INPUT_H + ELEMENT_GAP))
            .orElse(0);
    return inputFieldsH + BTN_H;
  }

  private void render(
      Wireframe wireframe, List<WireframeElement> elements, Graphics2D g, int height) {
    setupGraphics(g);
    drawBackground(g, height);
    drawTitle(wireframe, g);
    drawElements(elements, g);
  }

  private void setupGraphics(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setStroke(SKETCH_STROKE);
  }

  private void drawBackground(Graphics2D g, int height) {
    g.setColor(PAPER);
    g.fillRect(0, 0, SCREEN_WIDTH, height);
    g.setColor(INK);
    roughRect(g, 2, 2, SCREEN_WIDTH - 4, height - 4);
  }

  private void drawTitle(Wireframe wireframe, Graphics2D g) {
    g.setColor(INK);
    roughLine(g, 2, PADDING + TITLE_H, SCREEN_WIDTH - 2, PADDING + TITLE_H);
    g.setFont(TITLE_FONT);
    g.drawString(wireframe.friendlyName(), PADDING, PADDING + TITLE_H / 2 + 8);
  }

  private void drawElements(List<WireframeElement> elements, Graphics2D g) {
    var y = PADDING + TITLE_H + ELEMENT_GAP;
    for (var element : elements) {
      y = drawElement(element, g, y) + ELEMENT_GAP;
    }
  }

  private int drawElement(WireframeElement element, Graphics2D g, int y) {
    if (element instanceof Affordance affordance) {
      return drawAffordance(affordance, g, y);
    }
    return drawButton(((Artifact) element).friendlyName(), g, y);
  }

  private int drawAffordance(Affordance affordance, Graphics2D g, int y) {
    var fields = Optional.ofNullable(affordance.getInputFields()).orElse(List.of());
    var currentY = y;
    for (var field : fields) {
      currentY = drawInputField(field, g, currentY) + ELEMENT_GAP;
    }
    return drawButton(affordanceLabel(affordance), g, currentY);
  }

  String affordanceLabel(Affordance affordance) {
    return affordance.friendlyName();
  }

  private int drawInputField(InputField field, Graphics2D g, int y) {
    g.setFont(LABEL_FONT);
    g.setColor(LIGHT_INK);
    g.drawString(field.friendlyName(), PADDING, y + LABEL_H);
    var inputY = y + LABEL_H + LABEL_GAP;
    var inputW = SCREEN_WIDTH - 2 * PADDING;
    g.setColor(FIELD_TINT);
    g.fillRect(PADDING + 1, inputY + 1, inputW - 1, INPUT_H - 1);
    g.setColor(INK);
    roughRect(g, PADDING, inputY, inputW, INPUT_H);
    return inputY + INPUT_H;
  }

  private int drawButton(String name, Graphics2D g, int y) {
    var btnW = SCREEN_WIDTH - 2 * PADDING;
    g.setColor(BTN_TINT);
    g.fillRect(PADDING + 1, y + 1, btnW - 1, BTN_H - 1);
    g.setColor(INK);
    roughRect(g, PADDING, y, btnW, BTN_H);
    g.setFont(BTN_FONT);
    var fm = g.getFontMetrics();
    var textX = PADDING + (btnW - fm.stringWidth(name)) / 2;
    var textY = y + (BTN_H + fm.getAscent() - fm.getDescent()) / 2;
    g.drawString(name, textX, textY);
    return y + BTN_H;
  }

  private void roughRect(Graphics2D g, int x, int y, int w, int h) {
    var r = new Random((long) x * 1000 + y);
    roughLine(g, x + jitter(r), y + jitter(r), x + w + jitter(r), y + jitter(r));
    roughLine(g, x + w + jitter(r), y + jitter(r), x + w + jitter(r), y + h + jitter(r));
    roughLine(g, x + w + jitter(r), y + h + jitter(r), x + jitter(r), y + h + jitter(r));
    roughLine(g, x + jitter(r), y + h + jitter(r), x + jitter(r), y + jitter(r));
  }

  private void roughLine(Graphics2D g, int x1, int y1, int x2, int y2) {
    var r = new Random(x1 * 997L + y1 * 31L + x2 * 7L + y2);
    var path = new GeneralPath();
    path.moveTo(x1, y1);
    path.curveTo(
        x1 + (x2 - x1) / 3.0 + jitter(r),
        y1 + (y2 - y1) / 3.0 + jitter(r),
        x1 + 2 * (x2 - x1) / 3.0 + jitter(r),
        y1 + 2 * (y2 - y1) / 3.0 + jitter(r),
        x2,
        y2);
    g.draw(path);
  }

  private int jitter(Random r) {
    return r.nextInt(5) - 2;
  }
}
