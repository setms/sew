package org.setms.swe.domain.model.sdlc.code.html;

import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.format.Strings.toFriendlyName;
import static org.setms.km.domain.model.format.Strings.toKebabCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.technology.UiGenerator;
import org.setms.swe.domain.model.sdlc.ui.DesignSystem;
import org.setms.swe.domain.model.sdlc.ui.Property;
import org.setms.swe.domain.model.sdlc.ui.Style;
import org.setms.swe.domain.model.sdlc.ux.Affordance;
import org.setms.swe.domain.model.sdlc.ux.Container;
import org.setms.swe.domain.model.sdlc.ux.Feedback;
import org.setms.swe.domain.model.sdlc.ux.InputField;
import org.setms.swe.domain.model.sdlc.ux.View;
import org.setms.swe.domain.model.sdlc.ux.Wireframe;
import org.setms.swe.domain.model.sdlc.ux.WireframeElement;

public class ServerSideHtmlGenerator implements UiGenerator {

  @Override
  public List<CodeArtifact> generate(Wireframe wireframe, DesignSystem designSystem) {
    return List.of(htmlFor(wireframe, designSystem), cssFor(designSystem));
  }

  private CodeArtifact htmlFor(Wireframe wireframe, DesignSystem designSystem) {
    var containers = render(wireframe.getContainers(), this::htmlDiv);
    var code =
        """
        <!DOCTYPE html>
        <html data-artifact-name="%s">
        <head>
        <title>%s</title>
        <link rel="stylesheet" href="css/%s.css">
        </head>
        <body>
        %s</body>
        </html>
        """
            .formatted(
                wireframe.getName(),
                toFriendlyName(wireframe.getName()),
                toKebabCase(designSystem.getName()),
                containers);
    return new CodeArtifact(new FullyQualifiedName("", wireframe.getName())).setCode(code);
  }

  private String htmlDiv(Container container) {
    var children = render(container.getChildren(), this::htmlElement);
    return """
        <div id="%s">
        <h1>%s</h1>
        %s</div>
        """
        .formatted(toKebabCase(container.getName()), toFriendlyName(container.getName()), children);
  }

  private String htmlElement(WireframeElement element) {
    return switch (element) {
      case Container c -> htmlDiv(c);
      case Affordance a -> htmlAffordance(a);
      case View ignored -> "";
      case Feedback ignored -> "";
    };
  }

  private String htmlAffordance(Affordance affordance) {
    var inputs = render(affordance.getInputFields(), this::htmlInput);
    var formAttrs = formAttributes(affordance);
    return """
        <form%s>
        %s<button type="submit">%s</button>
        </form>
        """
        .formatted(formAttrs, inputs, toFriendlyName(affordance.getName()));
  }

  private String formAttributes(Affordance affordance) {
    return Optional.ofNullable(affordance.getCommand())
        .map(cmd -> " action=\"/%s\" method=\"post\"".formatted(toKebabCase(cmd.getId())))
        .orElse("");
  }

  private String htmlInput(InputField field) {
    return htmlInputType(field.getType())
        .map(type -> htmlLabeledInput(field.getName(), type))
        .orElse("");
  }

  private String htmlLabeledInput(String name, String type) {
    var kebabName = toKebabCase(name);
    return """
        <label for="%s">%s</label>
        <input type="%s" name="%s" id="%s">
        """
        .formatted(kebabName, toFriendlyName(name), type, kebabName, kebabName);
  }

  private Optional<String> htmlInputType(FieldType type) {
    return switch (type) {
      case TEXT -> Optional.of("text");
      case NUMBER -> Optional.of("number");
      case BOOLEAN -> Optional.of("checkbox");
      case DATE -> Optional.of("date");
      case TIME -> Optional.of("time");
      case DATETIME -> Optional.of("datetime-local");
      case ID -> Optional.empty();
      case SELECTION -> Optional.of("text");
    };
  }

  private <T> String render(List<T> items, Function<T, String> renderer) {
    return Optional.ofNullable(items).stream()
        .flatMap(Collection::stream)
        .map(renderer)
        .collect(joining());
  }

  private CodeArtifact cssFor(DesignSystem designSystem) {
    var declarationsBySelector = collectDeclarations(designSystem);
    var code =
        declarationsBySelector.entrySet().stream()
            .map(e -> cssRule(e.getKey(), e.getValue().stream().sorted().toList()))
            .collect(joining());
    return new CodeArtifact(new FullyQualifiedName("css", designSystem.getName())).setCode(code);
  }

  private Map<String, List<String>> collectDeclarations(DesignSystem designSystem) {
    var result = new TreeMap<String, List<String>>();
    result.put("form", baselineFormDeclarations());
    result.put("input", baselineFullWidthDeclarations());
    result.put("button", baselineFullWidthDeclarations());
    Optional.ofNullable(designSystem.getStyles()).stream()
        .flatMap(Collection::stream)
        .forEach(style -> collectStyleDeclarations(style, result));
    return result;
  }

  private List<String> baselineFormDeclarations() {
    var result = new ArrayList<String>();
    result.add(
        """
          display: flex;
        """);
    result.add(
        """
          flex-direction: column;
        """);
    return result;
  }

  private List<String> baselineFullWidthDeclarations() {
    var result = new ArrayList<String>();
    result.add(
        """
          padding: 0.5rem;
        """);
    result.add(
        """
          width: 100%;
        """);
    return result;
  }

  private void collectStyleDeclarations(Style style, Map<String, List<String>> declarations) {
    Optional.ofNullable(style.getProperties()).stream()
        .flatMap(Collection::stream)
        .forEach(property -> collectPropertyDeclaration(property, style.getName(), declarations));
  }

  private void collectPropertyDeclaration(
      Property property, String fallbackStyleName, Map<String, List<String>> declarations) {
    var translation = CssProperties.of(property.getName());
    var selector =
        translation.map(CssProperties.CssProperty::selector).orElse("." + fallbackStyleName);
    var cssProperty = translation.map(CssProperties.CssProperty::name).orElse(property.getName());
    declarations
        .computeIfAbsent(selector, ignored -> new ArrayList<>())
        .add(
            """
              %s: %s;
            """
                .formatted(cssProperty, property.getValue()));
  }

  private String cssRule(String selector, List<String> declarations) {
    return """
        %s {
        %s}
        """
        .formatted(selector, String.join("", declarations));
  }
}
