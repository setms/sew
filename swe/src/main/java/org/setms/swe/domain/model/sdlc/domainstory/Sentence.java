package org.setms.swe.domain.model.sdlc.domainstory;

import static org.setms.km.domain.model.validation.Level.ERROR;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Sentence extends Artifact {

  @NotEmpty private List<Link> parts;
  private String annotation;

  public Sentence(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  @Override
  public void validate(Location location, Collection<Diagnostic> diagnostics) {
    if (parts == null) {
      diagnostics.add(new Diagnostic(ERROR, "Missing parts", location));
      return;
    }
    if (parts.size() < 3) {
      diagnostics.add(
          new Diagnostic(
              ERROR, "Need at least 3 parts: actor, activity, and work object", location));
    }
    if (!parts.isEmpty() && !isActor(parts.getFirst())) {
      diagnostics.add(new Diagnostic(ERROR, "Sentence must start with an actor", location));
    }
    for (var i = 1; i < parts.size() - 1; i++) {
      var part = parts.get(i);
      var partLocation = location.plus("parts", parts, part);
      if (isActor(part)) {
        diagnostics.add(
            new Diagnostic(
                ERROR,
                "Actors can only occur at the beginning or end of a sentence",
                partLocation));
      }
      if (part.hasType("activity") != (i % 2 == 1)) {
        diagnostics.add(
            new Diagnostic(
                ERROR, "Must separate actors and work objects using activities", partLocation));
      }
    }
  }

  private boolean isActor(Link link) {
    return Stream.of("person", "people", "computerSystem").anyMatch(link::hasType);
  }

  public Stream<Link> parts() {
    return parts.stream();
  }
}
