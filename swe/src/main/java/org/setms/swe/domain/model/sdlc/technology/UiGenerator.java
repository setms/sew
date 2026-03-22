package org.setms.swe.domain.model.sdlc.technology;

import java.util.List;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.ui.DesignSystem;
import org.setms.swe.domain.model.sdlc.ux.Wireframe;

/** Generates UI code artifacts from a wireframe and a design system. */
public interface UiGenerator {

  List<CodeArtifact> generate(Wireframe wireframe, DesignSystem designSystem);
}
