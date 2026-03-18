package org.setms.swe.domain.model.sdlc.code.java;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.Artifact;
import org.setms.km.domain.model.format.Strings;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.design.FieldType;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.CodeGenerator;

public class JavaCodeGenerator extends JavaBaseCodeGenerator implements CodeGenerator {

  public JavaCodeGenerator(String topLevelPackage) {
    super(topLevelPackage);
  }

  public static Optional<CodeGenerator> from(
      ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    return JavaArtifactGenerator.topLevelPackage(inputs, diagnostics).map(JavaCodeGenerator::new);
  }

  @Override
  public List<CodeArtifact> generate(Aggregate aggregate, Entity root) {
    return generateDomainObjectFor(root, root);
  }

  @Override
  public List<CodeArtifact> generate(Command command, Entity payload) {
    return generateDomainObjectFor(command, payload);
  }

  private List<CodeArtifact> generateDomainObjectFor(Artifact artifact, Entity payload) {
    var packageName = packageFor(artifact, "domain.model");
    var className = artifact.getName();
    var components = componentsFor(payload);
    var imports = importsFor(payload);
    var importSection = imports.isEmpty() ? "" : imports + "\n\n";
    var code =
        """
            package %s;

            %spublic record %s(%s) {}
            """
            .formatted(packageName, importSection, className, components);
    return List.of(codeArtifact(packageName, className, code));
  }

  private String componentsFor(Entity payload) {
    return fieldsOf(payload)
        .map(f -> "%s %s".formatted(toJavaType(f.getType()), Strings.initLower(f.getName())))
        .collect(joining(", "));
  }

  private static Stream<Field> fieldsOf(Entity payload) {
    return Optional.ofNullable(payload).map(Entity::getFields).stream().flatMap(Collection::stream);
  }

  private String importsFor(Entity payload) {
    return fieldsOf(payload)
        .map(f -> toImport(f.getType()))
        .flatMap(Optional::stream)
        .distinct()
        .sorted()
        .map("import %s;"::formatted)
        .collect(joining("\n"));
  }

  private static Optional<String> toImport(FieldType type) {
    return switch (type) {
      case DATETIME -> Optional.of("java.time.LocalDateTime");
      case DATE -> Optional.of("java.time.LocalDate");
      case TIME -> Optional.of("java.time.LocalTime");
      default -> Optional.empty();
    };
  }

  @Override
  public List<CodeArtifact> generate(Event event, Entity payload) {
    return generateDomainObjectFor(event, payload);
  }

  @Override
  public List<CodeArtifact> generate(
      Aggregate aggregate,
      Command command,
      Entity commandPayload,
      Event event,
      Entity eventPayload) {
    var packageName = packageFor(aggregate, "domain.services");
    var repositoryName = aggregate.getName() + "Repository";
    var serviceName = aggregate.getName() + "Service";
    var entityName = aggregate.domainObjectName();
    var commandFqn = "%s.%s".formatted(packageFor(command, "domain.model"), command.getName());
    var entityFqn = "%s.%s".formatted(packageFor(aggregate, "domain.model"), entityName);
    var eventFqn = "%s.%s".formatted(packageFor(event, "domain.model"), event.getName());
    var repositoryImport = "import %s;".formatted(entityFqn);
    var serviceImports =
        Stream.of(commandFqn, eventFqn)
            .sorted()
            .map("import %s;"::formatted)
            .collect(joining("\n"));
    var paramName = Strings.initLower(command.getName());
    var returnExpression =
        buildReturnExpression(event.getName(), eventPayload, commandPayload, paramName);
    return List.of(
        repositoryInterface(packageName, repositoryImport, repositoryName, entityName),
        serviceInterface(
            packageName,
            serviceName,
            command.getName(),
            event.getName(),
            serviceImports,
            paramName),
        serviceImpl(
            packageName,
            serviceName,
            repositoryName,
            command.getName(),
            event.getName(),
            serviceImports,
            paramName,
            returnExpression));
  }

  private CodeArtifact repositoryInterface(
      String packageName, String repositoryImport, String repositoryName, String aggregateName) {
    var code =
        """
        package %s;

        %s
        import java.util.Collection;

        public interface %s {

          Collection<%s> loadAll();

          void insert(%s aggregate);

          void update(%s aggregate);
        }
        """
            .formatted(
                packageName,
                repositoryImport,
                repositoryName,
                aggregateName,
                aggregateName,
                aggregateName);
    return codeArtifact(packageName, repositoryName, code);
  }

  private String buildReturnExpression(
      String eventName, Entity eventPayload, Entity commandPayload, String paramName) {
    if (eventPayload == null) {
      return "null";
    }
    var eventFields = fieldsOf(eventPayload).toList();
    if (eventFields.isEmpty()) {
      return "new %s()".formatted(eventName);
    }
    var commandFieldNames = fieldsOf(commandPayload).map(Field::getName).collect(toSet());
    if (commandFieldNames.containsAll(eventFields.stream().map(Field::getName).collect(toSet()))) {
      var params =
          eventFields.stream()
              .map(f -> "%s.%s()".formatted(paramName, Strings.initLower(f.getName())))
              .collect(joining(", "));
      return "new %s(%s)".formatted(eventName, params);
    }
    return "null";
  }

  private CodeArtifact serviceInterface(
      String packageName,
      String serviceName,
      String commandName,
      String eventName,
      String imports,
      String paramName) {
    var code =
        """
        package %s;

        %s

        public interface %s {

          %s accept(%s %s);
        }
        """
            .formatted(packageName, imports, serviceName, eventName, commandName, paramName);
    return codeArtifact(packageName, serviceName, code);
  }

  private CodeArtifact serviceImpl(
      String packageName,
      String serviceName,
      String repositoryName,
      String commandName,
      String eventName,
      String imports,
      String paramName,
      String returnExpression) {
    var code =
        """
        package %s;

        %s
        import lombok.RequiredArgsConstructor;

        @RequiredArgsConstructor
        public class %sImpl implements %s {

          private final %s repository;

          @Override
          public %s accept(%s %s) {
            return %s;
          }
        }
        """
            .formatted(
                packageName,
                imports,
                serviceName,
                serviceName,
                repositoryName,
                eventName,
                commandName,
                paramName,
                returnExpression);
    return codeArtifact(packageName, serviceName + "Impl", code);
  }
}
