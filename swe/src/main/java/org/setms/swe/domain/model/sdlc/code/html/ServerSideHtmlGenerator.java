package org.setms.swe.domain.model.sdlc.code.html;

import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.List;
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
    return List.of(cssFor(designSystem), htmlFor(wireframe));
  }

  private CodeArtifact cssFor(DesignSystem designSystem) {
    var code = render(designSystem.getStyles(), this::cssRule);
    return new CodeArtifact(new FullyQualifiedName("", designSystem.getName())).setCode(code);
  }

  private String cssRule(Style style) {
    var properties = render(style.getProperties(), this::cssProperty);
    return ".%s {\n%s}\n".formatted(style.getName(), properties);
  }

  private String cssProperty(Property property) {
    return "  %s: %s;\n".formatted(property.getName(), property.getValue());
  }

  private CodeArtifact htmlFor(Wireframe wireframe) {
    var containers = render(wireframe.getContainers(), this::htmlDiv);
    var code =
        """
        <!DOCTYPE html>
        <html>
        <head><title>%s</title></head>
        <body>
        %s</body>
        </html>
        """
            .formatted(wireframe.getName(), containers);
    return new CodeArtifact(new FullyQualifiedName("", wireframe.getName())).setCode(code);
  }

  private String htmlDiv(Container container) {
    return "<div id=\"%s\"></div>\n".formatted(container.getName());
  }

  private <T> String render(List<T> items, Function<T, String> renderer) {
    return Optional.ofNullable(items).stream()
        .flatMap(Collection::stream)
        .map(renderer)
        .collect(joining());
  }
}
