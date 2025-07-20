package org.setms.km.domain.model.validation;

import java.util.Collection;

public interface Validatable {

  Location toLocation();

  Location appendTo(Location location);

  boolean starts(Location location);

  void validate(Location location, Collection<Diagnostic> diagnostics);
}
