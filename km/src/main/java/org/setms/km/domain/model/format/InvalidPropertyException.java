package org.setms.km.domain.model.format;

import static org.setms.km.domain.model.format.Strings.initLower;

import java.util.Collection;
import java.util.LinkedHashSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.setms.km.domain.model.validation.Locatable;
import org.setms.km.domain.model.validation.Location;

@RequiredArgsConstructor
public class InvalidPropertyException extends IllegalArgumentException {

  @Getter private final Collection<Locatable> targets = new LinkedHashSet<>();
  @Getter private final String property;

  public InvalidPropertyException(Object target, String property, String message, Object... args) {
    super(message.formatted(args));
    this.property = property;
    this.targets.add(toLocatable(target));
  }

  private Locatable toLocatable(Object target) {
    return target instanceof Locatable locatable
        ? locatable
        : new Locatable() {

          private final String segment = initLower(target.getClass().getSimpleName());

          @Override
          public Location toLocation() {
            return new Location(segment);
          }

          @Override
          public Location appendTo(Location location) {
            return location.plus(segment);
          }
        };
  }

  public InvalidPropertyException(InvalidPropertyException source, Object target) {
    super(source.getMessage());
    this.property = source.property;
    this.targets.add(toLocatable(target));
    this.targets.addAll((source.targets));
  }

  public Location toLocation() {
    Location result = null;
    for (var target : targets) {
      result = result == null ? target.toLocation() : target.appendTo(result);
    }
    return result.plus(property);
  }
}
