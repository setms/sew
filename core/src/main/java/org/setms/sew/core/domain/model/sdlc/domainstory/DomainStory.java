package org.setms.sew.core.domain.model.sdlc.domainstory;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DomainStory extends NamedObject {

  @NotEmpty private String description;
  @NotNull private Granularity granularity = Granularity.FINE;
  @NotNull private PointInTime pointInTime = PointInTime.TOBE;
  @NotNull private Purity purity = Purity.PURE;
  private String annotation;
  @NotEmpty private List<Sentence> sentences;

  public DomainStory(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
