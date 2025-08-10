package org.setms.swe.domain.model.sdlc.acceptance;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

@Getter
@Setter
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract sealed class Variable<T, D> extends Artifact
    permits ElementVariable, FieldVariable {

  private T type;
  private List<D> definitions;

  public Variable(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
