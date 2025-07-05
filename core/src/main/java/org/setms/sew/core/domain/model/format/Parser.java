package org.setms.sew.core.domain.model.format;

import static org.setms.sew.core.domain.model.format.Strings.initUpper;
import static org.setms.sew.core.domain.model.format.Validation.validate;
import static org.setms.sew.core.domain.model.tool.Level.ERROR;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.atteo.evo.inflector.English;
import org.setms.sew.core.domain.model.sdlc.Enums;
import org.setms.sew.core.domain.model.sdlc.FullyQualifiedName;
import org.setms.sew.core.domain.model.sdlc.NamedObject;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.InputSource;
import org.setms.sew.core.domain.model.tool.Location;

public interface Parser {

  default <T extends NamedObject> T parse(InputStream input, Class<T> type, boolean validate)
      throws IOException {
    return convert(parse(input), type, validate);
  }

  RootObject parse(InputStream input) throws IOException;

  default <T extends NamedObject> T convert(RootObject object, Class<T> type, boolean validate) {
    if (object == null) {
      return null;
    }
    if (!object.getType().equalsIgnoreCase(type.getSimpleName())) {
      throw new IllegalArgumentException(
          "Can't parse %s from %s".formatted(type.getName(), object.getType()));
    }
    var result = parseNamedObject(object, type, object.getScope(), object.getName(), validate);
    if (validate) {
      try {
        validate(result);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("%s: %s".formatted(result.getName(), e.getMessage()), e);
      }
    }
    return result;
  }

  default <T extends NamedObject> T parseNamedObject(
      DataObject<?> source, Class<T> type, String scope, Object name, boolean validate) {
    try {
      var result =
          type.getConstructor(FullyQualifiedName.class)
              .newInstance(new FullyQualifiedName("%s.%s".formatted(scope, name)));
      setProperties(source, result, validate);
      return result;
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException(e);
    }
  }

  default void setProperties(DataObject<?> source, Object target, boolean validate) {
    source.properties(
        (name, value) -> setProperty(name, convert(name, value, target, validate), target));
  }

  default Object convert(String name, DataItem value, Object target, boolean validate) {
    return switch (value) {
      case null -> null;
      case DataString string -> string.getValue();
      case DataEnum dataEnum -> dataEnum.getName();
      case DataList list ->
          list.map(item -> convert(name, item, target, validate)).filter(Objects::nonNull).toList();
      case NestedObject object -> createObject(object, name, target, validate);
      case Reference reference -> {
        var attributes = new HashMap<String, List<Pointer>>();
        reference
            .getAttributes()
            .forEach(
                (key, references) ->
                    attributes.put(
                        key,
                        references.stream()
                            .map(ref -> new Pointer(ref.getType(), ref.getId()))
                            .toList()));
        yield new Pointer(reference.getType(), reference.getId(), attributes);
      }
      default ->
          throw new UnsupportedOperationException(
              "Unexpected value of type " + value.getClass().getSimpleName());
    };
  }

  @SuppressWarnings("unchecked")
  default NamedObject createObject(
      NestedObject source, String name, Object parent, boolean validate) {
    return findClassNamed(initUpper(name), parent)
        .filter(NamedObject.class::isAssignableFrom)
        .map(c -> (Class<NamedObject>) c)
        .map(type -> parseNamedObject(source, type, name, source.getName(), validate))
        .orElse(null);
  }

  default Optional<? extends Class<?>> findClassNamed(String name, Object parent) {
    try (var scanResult =
        new ClassGraph()
            .enableClassInfo()
            .acceptPackages(parent.getClass().getPackageName())
            .scan()) {
      return scanResult.getAllClasses().stream()
          .filter(c -> matchesName(c.getSimpleName(), name))
          .map(ClassInfo::loadClass)
          .findFirst();
    }
  }

  default boolean matchesName(String name, String candidate) {
    return name.equalsIgnoreCase(candidate)
        || name.equalsIgnoreCase(English.plural(candidate))
        || English.plural(name).equalsIgnoreCase(candidate);
  }

  default void setProperty(String name, Object targetValue, Object target) {
    var setter = "set%s".formatted(initUpper(name));
    try {
      var method = findMethod(setter, targetValue, target.getClass());
      if (method != null) {
        var parameterType = method.getParameters()[0].getType();
        if (targetValue != null
            && Collection.class.isAssignableFrom(parameterType)
            && !parameterType.isAssignableFrom(targetValue.getClass())) {
          method.invoke(target, toCollection(targetValue, parameterType, target, name));
        } else if (targetValue != null && parameterType.isEnum()) {
          method.invoke(target, toEnum(targetValue, parameterType));
        } else if (targetValue != null && parameterType.equals(Boolean.class)
            || parameterType.equals(boolean.class)) {
          var booleanValue = Boolean.parseBoolean(targetValue.toString());
          method.invoke(target, booleanValue);
        } else {
          method.invoke(target, targetValue);
        }
      }
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException(
          "Failed to set %s.%s".formatted(target.getClass(), name), e);
    }
  }

  default Enum<?> toEnum(Object targetValue, Class<?> parameterType) {
    return Arrays.stream(parameterType.getEnumConstants())
        .map(Enum.class::cast)
        .filter(c -> c.name().equalsIgnoreCase(targetValue.toString()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid value: " + targetValue));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  default Collection<Object> toCollection(
      Object targetValue, Class<?> type, Object target, String name) {
    if (type == Collection.class) {
      return targetValue instanceof Collection collection ? collection : List.of(targetValue);
    }
    if (Enums.class.isAssignableFrom(type)) {
      var targetValues = (Collection) targetValue;
      if (targetValues.isEmpty()) {
        return null;
      }
      var getter = findMethod("get%s".formatted(initUpper(name)), null, target.getClass());
      try {
        var result = (Enums) getter.invoke(target);
        var itemType = result.getType();
        targetValues.stream().map(v -> toEnum(v, itemType)).forEach(result::add);
        return result;
      } catch (ReflectiveOperationException e) {
        throw new IllegalArgumentException("Failed to determine enums type", e);
      }
    }
    if (List.class.isAssignableFrom(type)) {
      return targetValue instanceof Collection collection
          ? new ArrayList<>(collection)
          : List.of(targetValue);
    }
    if (Set.class.isAssignableFrom(type)) {
      return targetValue instanceof Collection collection
          ? new LinkedHashSet<>(collection)
          : Set.of(targetValue);
    }
    throw new UnsupportedOperationException("Unsupported collection type " + type.getName());
  }

  default Method findMethod(String name, Object targetValue, Class<?> type) {
    var methods =
        Arrays.stream(type.getMethods()).filter(m -> matchesName(name, m.getName())).toList();
    if (methods.isEmpty()) {
      // Ignore unsupported property (Postel's Law)
      return null;
    }
    if (methods.size() == 1) {
      return methods.getFirst();
    }
    return methods.stream()
        .filter(m -> m.getParameterCount() == 1)
        .filter(
            m ->
                targetValue == null
                    || m.getParameters()[0].getType().isAssignableFrom(targetValue.getClass()))
        .findFirst()
        .orElse(null);
  }

  default <T extends NamedObject> Stream<T> parseMatching(
      InputSource source,
      Glob glob,
      Class<T> type,
      boolean validate,
      Collection<Diagnostic> diagnostics) {
    return source.matching(glob).stream()
        .map(
            inputSource -> {
              try (var inputStream = inputSource.open()) {
                return parse(inputStream, type, validate);
              } catch (Exception e) {
                diagnostics.add(
                    new Diagnostic(
                        ERROR,
                        e.getMessage(),
                        Optional.ofNullable(inputSource.name())
                            .map(n -> n.split("\\."))
                            .filter(a -> a.length == 2)
                            .map(a -> new String[] {a[1], a[0]})
                            .map(Location::new)
                            .orElse(null)));
                return null;
              }
            })
        .filter(Objects::nonNull);
  }
}
