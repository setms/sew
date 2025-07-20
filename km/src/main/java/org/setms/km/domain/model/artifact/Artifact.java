package org.setms.km.domain.model.artifact;

import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.format.Strings.toFriendlyName;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;

@RequiredArgsConstructor
@EqualsAndHashCode
public abstract class Artifact
    implements Comparable<Artifact>, org.setms.km.domain.model.validation.Validatable {

  @Getter @Valid private final FullyQualifiedName fullyQualifiedName;

  public String type() {
    return initLower(getClass().getSimpleName());
  }

  public String getPackage() {
    return fullyQualifiedName.getPackage();
  }

  public String getName() {
    return fullyQualifiedName.getName();
  }

  public String friendlyName() {
    return toFriendlyName(getName());
  }

  public Pointer pointerTo() {
    return new Pointer(type(), getName());
  }

  @Override
  public Location toLocation() {
    return new Location(List.of(getPackage(), type(), getName()));
  }

  @Override
  public Location appendTo(Location location) {
    return location.plus(List.of(type(), getName()));
  }

  @Override
  public boolean starts(Location location) {
    var segments = location.segments();
    return segments.get(0).equals(getPackage())
        && segments.get(1).equals(type())
        && segments.get(2).equals(getName());
  }

  @Override
  public void validate(Location location, Collection<Diagnostic> diagnostics) {
    // Base classes can add additional validation here
  }

  @Override
  public String toString() {
    return fullyQualifiedName.toString();
  }

  @Override
  public int compareTo(Artifact that) {
    return this.fullyQualifiedName.compareTo(that.fullyQualifiedName);
  }
}
