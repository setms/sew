package org.setms.km.domain.model.validation;

public interface Locatable {

  Location toLocation();

  Location appendTo(Location location);
}
