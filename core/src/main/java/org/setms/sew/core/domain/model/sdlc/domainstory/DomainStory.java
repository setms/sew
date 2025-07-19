package org.setms.sew.core.domain.model.sdlc.domainstory;

import static org.setms.sew.core.domain.model.sdlc.domainstory.Purity.DIGITIALIZED;
import static org.setms.sew.core.domain.model.sdlc.domainstory.Purity.PURE;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.validation.Diagnostic;
import org.setms.sew.core.domain.model.validation.Location;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DomainStory extends NamedObject {

  @NotEmpty private String description;
  @NotNull private Granularity granularity = Granularity.FINE;
  @NotNull private PointInTime pointInTime = PointInTime.TOBE;
  private String annotation;
  @NotEmpty private List<Sentence> sentences;

  public DomainStory(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }

  public Purity purity() {
    return sentences.stream()
        .map(Sentence::getParts)
        .flatMap(Collection::stream)
        .anyMatch(Pointer.testType("computerSystem")) ? DIGITIALIZED : PURE;
  }

  @Override
  public void validate(Location location, Collection<Diagnostic> diagnostics) {
    sentences.forEach(sentence -> sentence.validate(location.plus(sentence), diagnostics));
  }
}
