package org.setms.sew.core.domain.model.sdlc.ddd;

import java.util.List;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

public record ResolvedSequence(List<NamedObject> items) {}
