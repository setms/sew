package org.setms.swe.domain.model.sdlc.code.html;

import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.format.Strings.toFriendlyName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.technology.UiGenerator;
import org.setms.swe.domain.model.sdlc.ui.DesignSystem;
import org.setms.swe.domain.model.sdlc.ui.Property;
import org.setms.swe.domain.model.sdlc.ui.Style;
import org.setms.swe.domain.model.sdlc.ux.Container;
import org.setms.swe.domain.model.sdlc.ux.Wireframe;

public class ServerSideHtmlGenerator implements UiGenerator {

  @Override
  public List<CodeArtifact> generate(Wireframe wireframe, DesignSystem designSystem) {
    return List.of(htmlFor(wireframe), cssFor(designSystem));
  }

  private CodeArtifact htmlFor(Wireframe wireframe) {
    var containers = render(wireframe.getContainers(), this::htmlDiv);
    var code =
        """
        <!DOCTYPE html>
        <html data-artifact-name="%s">
        <head><title>%s</title></head>
        <body>
        %s</body>
        </html>
        """
            .formatted(wireframe.getName(), toFriendlyName(wireframe.getName()), containers);
    return new CodeArtifact(new FullyQualifiedName("", wireframe.getName())).setCode(code);
  }

  private String htmlDiv(Container container) {
    return """
        <div id="%s"></div>
        """
        .formatted(container.getName());
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
            .map(e -> cssRule(e.getKey(), e.getValue()))
            .collect(joining());
    return new CodeArtifact(new FullyQualifiedName("css", designSystem.getName())).setCode(code);
  }

  private Map<String, List<String>> collectDeclarations(DesignSystem designSystem) {
    var result = new LinkedHashMap<String, List<String>>();
    Optional.ofNullable(designSystem.getStyles()).stream()
        .flatMap(Collection::stream)
        .forEach(style -> collectStyleDeclarations(style, result));
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
