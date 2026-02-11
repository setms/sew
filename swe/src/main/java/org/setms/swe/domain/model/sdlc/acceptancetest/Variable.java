package org.setms.swe.domain.model.sdlc.acceptancetest;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.artifact.FullyQualifiedName;

@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract sealed class Variable<T, D, V extends Variable<T, D, V>> extends Artifact
    permits ElementVariable, FieldVariable {

  private final Function<String, T> toTypeConverter;
  private final Function<T, String> fromTypeConverter;
  private String type;
  @Getter @Setter private List<D> definitions;

  public Variable(
      FullyQualifiedName fullyQualifiedName,
      Function<String, T> toTypeConverter,
      Function<T, String> fromTypeConverter) {
    super(fullyQualifiedName);
    this.toTypeConverter = toTypeConverter;
    this.fromTypeConverter = fromTypeConverter;
  }

  public T getType() {
    return Optional.ofNullable(type).map(toTypeConverter).orElse(null);
  }

  @SuppressWarnings("unchecked")
  public V setType(T type) {
    this.type =
        type instanceof String value
            ? value
            : Optional.ofNullable(type).map(fromTypeConverter).orElse(null);
    return (V) this;
  }
}
