package org.setms.swe.domain.model.sdlc.code.html;

import static java.util.Collections.emptyList;

import java.util.List;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.technology.UiGenerator;
import org.setms.swe.domain.model.sdlc.ui.DesignSystem;
import org.setms.swe.domain.model.sdlc.ux.Wireframe;

public class ServerSideHtmlGenerator implements UiGenerator {

  @Override
  public List<CodeArtifact> generate(Wireframe wireframe, DesignSystem designSystem) {
    return emptyList();
  }
}
