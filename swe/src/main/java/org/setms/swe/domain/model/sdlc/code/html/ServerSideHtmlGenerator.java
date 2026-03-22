package org.setms.swe.domain.model.sdlc.code.html;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    var code =
        Optional.ofNullable(designSystem.getStyles()).stream()
            .flatMap(Collection::stream)
            .map(this::cssRule)
            .reduce("", (a, b) -> a + b);
    return new CodeArtifact(new FullyQualifiedName("", designSystem.getName())).setCode(code);
  }

  private String cssRule(Style style) {
    var properties =
        Optional.ofNullable(style.getProperties()).stream()
            .flatMap(Collection::stream)
            .map(this::cssProperty)
            .reduce("", (a, b) -> a + b);
    return ".%s {\n%s}\n".formatted(style.getName(), properties);
  }

  private String cssProperty(Property property) {
    return "  %s: %s;\n".formatted(property.getName(), property.getValue());
  }

  private CodeArtifact htmlFor(Wireframe wireframe) {
    var containers =
        Optional.ofNullable(wireframe.getContainers()).stream()
            .flatMap(Collection::stream)
            .map(this::htmlDiv)
            .reduce("", (a, b) -> a + b);
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
}
