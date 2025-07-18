package org.setms.sew.core.domain.model.sdlc;

import static org.setms.sew.core.domain.model.format.Strings.initLower;
import static org.setms.sew.core.domain.model.format.Strings.toFriendlyName;

import jakarta.validation.Valid;
import java.util.Collection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.sew.core.domain.model.validation.Diagnostic;
import org.setms.sew.core.domain.model.validation.Location;

@RequiredArgsConstructor
@EqualsAndHashCode
public abstract class NamedObject implements Comparable<NamedObject> {

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

  public void validate(Location location, Collection<Diagnostic> diagnostics) {
    // Base classes can add additional validation here
  }

  @Override
  public String toString() {
    return fullyQualifiedName.toString();
  }

  @Override
  public int compareTo(NamedObject that) {
    return this.fullyQualifiedName.compareTo(that.fullyQualifiedName);
  }
}
