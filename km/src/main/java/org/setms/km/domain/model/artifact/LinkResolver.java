package org.setms.km.domain.model.artifact;

public interface LinkResolver {

  default Artifact resolve(Link link) {
    return resolve(link, null);
  }

  Artifact resolve(Link link, String defaultType);
}
