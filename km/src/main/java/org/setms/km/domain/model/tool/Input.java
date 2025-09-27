package org.setms.km.domain.model.tool;

import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Format;

public interface Input<T extends Artifact> {

  String name();

  Format format();

  Class<T> type();

  boolean targets(Artifact artifact);

  boolean matches(String path);

  String path();

  String extension();
}
