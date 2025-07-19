package org.setms.km.domain.model.artifact;

public interface PointerResolver {

  default Artifact resolve(Pointer pointer) {
    return resolve(pointer, null);
  }

  Artifact resolve(Pointer pointer, String defaultType);
}
