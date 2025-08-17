package org.setms.km.domain.model.diagram;

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
public class Foo extends Artifact {

  public Foo(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
