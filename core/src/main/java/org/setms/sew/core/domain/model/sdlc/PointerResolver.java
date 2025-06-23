package org.setms.sew.core.domain.model.sdlc;


public interface PointerResolver {

  default NamedObject resolve(Pointer pointer) {
    return resolve(pointer, null);
  }

  NamedObject resolve(Pointer pointer, String defaultType);

}
