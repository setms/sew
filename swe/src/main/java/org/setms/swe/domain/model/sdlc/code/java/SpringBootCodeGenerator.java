package org.setms.swe.domain.model.sdlc.code.java;

import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.format.Strings.initLower;
import static org.setms.km.domain.model.format.Strings.initUpper;
import static org.setms.km.domain.model.format.Strings.toKebabCase;
import static org.setms.km.domain.model.format.Strings.toSnakeCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.architecture.Framework;
import org.setms.swe.domain.model.sdlc.architecture.TopicProvider;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.database.DatabaseSchema;
import org.setms.swe.domain.model.sdlc.database.postgresql.PostgreSql;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.design.Field;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.CodeBuilder;
import org.setms.swe.domain.model.sdlc.technology.Database;
import org.setms.swe.domain.model.sdlc.technology.FrameworkCodeGenerator;

@Slf4j
public class SpringBootCodeGenerator extends JavaBaseCodeGenerator
    implements FrameworkCodeGenerator, TopicProvider {

  private static final Map<Class<? extends Database>, String> DRIVER_DEPENDENCIES =
      Map.of(PostgreSql.class, "org.postgresql:postgresql");
  private static final String NL = System.lineSeparator();
  private static final String MAIN_CLASS_CODE =
      """
      package %1$s;

      import org.springframework.boot.SpringApplication;
      import org.springframework.boot.autoconfigure.SpringBootApplication;

      @SpringBootApplication
      public class %2$s {

        public static void main(String... args) {
          SpringApplication.run(%2$s.class, args);
        }
      }
      """;

  private final CodeBuilder codeBuilder;

  @SuppressWarnings("unused") // Called by ServiceLoader
  public SpringBootCodeGenerator() {
    this(null, null);
  }

  public SpringBootCodeGenerator(String topLevelPackage, CodeBuilder codeBuilder) {
    super(topLevelPackage);
    this.codeBuilder = codeBuilder;
  }

  @Override
  public boolean isValidChoice(String topic, String choice) {
    return Framework.TOPIC.equals(topic) && "Spring Boot".equals(choice);
  }

  @Override
  public List<CodeArtifact> generateEntityFor(
      Aggregate aggregate, DatabaseSchema schema, Database database, Resource<?> resource) {
    ensureSpringBootJpaDependency(resource);
    ensureMapStructBuildPlugin(resource);
    ensureDriverDependency(database, resource);
    ensureApplicationLocalYml(database, resource);
    return entityArtifactsFor(aggregate, schema, database);
  }

  private List<CodeArtifact> entityArtifactsFor(
      Aggregate aggregate, DatabaseSchema schema, Database database) {
    var dbPackage = getTopLevelPackage() + ".outbound.db";
    var entityName = schema.getName() + "Entity";
    var jpaRepositoryName = schema.getName() + "JpaRepository";
    var domainRepositoryName = aggregate.getName() + "Repository";
    var mapperName = schema.getName() + "Mapper";
    var rootEntityName = aggregate.domainObjectName();
    var result = new ArrayList<CodeArtifact>();
    result.add(entityFor(dbPackage, entityName, database.extractFieldsFrom(schema)));
    result.add(jpaRepositoryFor(dbPackage, entityName, jpaRepositoryName));
    result.add(mapperFor(dbPackage, entityName, mapperName, aggregate, rootEntityName));
    result.add(
        domainRepositoryFor(
            dbPackage, domainRepositoryName, jpaRepositoryName, mapperName, rootEntityName));
    return result;
  }

  private CodeArtifact domainRepositoryFor(
      String dbPackage,
      String domainRepositoryName,
      String jpaRepositoryName,
      String mapperName,
      String aggregateName) {
    var aggregateFqn = "%s.%s.%s".formatted(getTopLevelPackage(), "domain.model", aggregateName);
    var implName = domainRepositoryName + "Impl";
    var interfaceFqn =
        "%s.%s.%s".formatted(getTopLevelPackage(), "domain.services", domainRepositoryName);
    var code =
        """
        package %s;

        import %s;
        import %s;
        import java.util.Collection;
        import lombok.RequiredArgsConstructor;
        import org.springframework.stereotype.Service;

        @Service
        @RequiredArgsConstructor
        public class %s implements %s {

          private final %s jpaRepository;
          private final %s mapper;

          @Override
          public Collection<%s> loadAll() {
            return jpaRepository.findAll().stream().map(mapper::toAggregate).toList();
          }

          @Override
          public void insert(%s aggregate) {
            jpaRepository.save(mapper.toEntity(aggregate));
          }

          @Override
          public void update(%s aggregate) {
            jpaRepository.save(mapper.toEntity(aggregate));
          }
        }
        """
            .formatted(
                dbPackage,
                aggregateFqn,
                interfaceFqn,
                implName,
                domainRepositoryName,
                jpaRepositoryName,
                mapperName,
                aggregateName,
                aggregateName,
                aggregateName);
    return codeArtifact(dbPackage, implName, code);
  }

  private void ensureSpringBootJpaDependency(Resource<?> resource) {
    codeBuilder.addDependency(
        "org.springframework.boot:spring-boot-starter-data-jpa", resource.root());
    codeBuilder.configureTask(
        "bootRun", List.of("systemProperty 'spring.profiles.active', 'local'"), resource.root());
  }

  private void ensureMapStructBuildPlugin(Resource<?> resource) {
    codeBuilder.addBuildPlugin("com.github.akazver.mapstruct", resource.root());
    codeBuilder.configureTask(
        "mapstruct",
        List.of(
            "defaultComponentModel = \"spring\"",
            "defaultInjectionStrategy = \"constructor\"",
            "unmappedSourcePolicy = \"IGNORE\""),
        resource.root());
  }

  private void ensureDriverDependency(Database database, Resource<?> resource) {
    Optional.ofNullable(DRIVER_DEPENDENCIES.get(database.getClass()))
        .ifPresent(dep -> codeBuilder.addRuntimeDependency(dep, resource.root()));
  }

  private void ensureApplicationLocalYml(Database database, Resource<?> resource) {
    database
        .localDataSourceUrl(applicationName())
        .ifPresent(url -> writeApplicationLocalYml(url, resource));
  }

  private void writeApplicationLocalYml(String dataSourceUrl, Resource<?> resource) {
    try {
      resource
          .root()
          .select("src/main/resources/application-local.yml")
          .writeAsString(
              """
              spring:
                datasource:
                  password: password
                  username: postgres
                  url: %s
              """
                  .formatted(dataSourceUrl));
    } catch (Exception e) {
      log.error("Failed to write application-local.yml", e);
    }
  }

  private CodeArtifact entityFor(
      String entityPackage, String entityName, Collection<Field> fields) {
    var code =
        """
        package %s;

        import jakarta.persistence.Column;
        import jakarta.persistence.Entity;
        import jakarta.persistence.GeneratedValue;
        import jakarta.persistence.GenerationType;
        import jakarta.persistence.Id;
        %simport lombok.Getter;
        import lombok.Setter;

        @Entity
        @Getter
        @Setter
        public class %s {

          @Id
          @GeneratedValue(strategy = GenerationType.UUID)
        %s}
        """
            .formatted(entityPackage, importsFor(fields), entityName, toFields(fields));
    return codeArtifact(entityPackage, entityName, code);
  }

  private String importsFor(Collection<Field> fields) {
    return fields.stream()
        .map(Field::getType)
        .distinct()
        .map(this::toJavaType)
        .map(this::toImport)
        .filter(Objects::nonNull)
        .map("import %s;%n"::formatted)
        .sorted()
        .collect(joining());
  }

  private String toImport(String type) {
    return switch (type) {
      case "LocalTime", "LocalDate", "LocalDateTime" -> "java.time.%s".formatted(type);
      case "UUID" -> "java.util.%s".formatted(type);
      default -> null;
    };
  }

  private String toFields(Collection<Field> fields) {
    if (fields.isEmpty()) {
      return "";
    }
    return "%s;%s"
        .formatted(
            fields.stream()
                .map(
                    field ->
                        "  @Column(name = \"%s\")%n  private %s"
                            .formatted(toSnakeCase(field.getName()), toField(field)))
                .collect(joining(";" + NL + NL)),
            NL);
  }

  private String toField(Field field) {
    return "%s %s".formatted(toJavaType(field.getType()), initLower(field.getName()));
  }

  private CodeArtifact jpaRepositoryFor(
      String entityPackage, String entityName, String repositoryName) {
    var code =
        """
        package %s;

        import java.util.UUID;
        import org.springframework.data.jpa.repository.JpaRepository;

        public interface %s extends JpaRepository<%s, UUID> {}
        """
            .formatted(entityPackage, repositoryName, entityName);
    return codeArtifact(entityPackage, repositoryName, code);
  }

  private CodeArtifact mapperFor(
      String entityPackage,
      String entityName,
      String mapperName,
      Aggregate aggregate,
      String aggregateName) {
    var aggregateFqn = "%s.%s".formatted(packageFor(aggregate, "domain.model"), aggregateName);
    var code =
        """
        package %s;

        import %s;
        import org.mapstruct.Mapper;

        @Mapper
        public interface %s {

          %s toAggregate(%s entity);

          %s toEntity(%s aggregate);
        }
        """
            .formatted(
                entityPackage,
                aggregateFqn,
                mapperName,
                aggregateName,
                entityName,
                entityName,
                aggregateName);
    return codeArtifact(entityPackage, mapperName, code);
  }

  @Override
  public List<CodeArtifact> generateEndpointFor(
      Resource<?> resource,
      Aggregate aggregate,
      Command command,
      Entity commandPayload,
      Event event) {
    ensureSpringBootWebDependency(resource);
    var result = new ArrayList<CodeArtifact>();
    ensureMainClass(resource).ifPresent(result::add);
    ensureDomainServiceIsSpringBean(aggregate, resource).ifPresent(result::add);
    result.add(controllerFor(aggregate, command, event));
    return result;
  }

  private void ensureSpringBootWebDependency(Resource<?> resource) {
    codeBuilder.addBuildPlugin("org.springframework.boot", resource);
    codeBuilder.enableBuildPlugin("io.spring.dependency-management", resource);
    codeBuilder.addDependency("org.springframework.boot:spring-boot-starter-web", resource);
  }

  private Optional<CodeArtifact> ensureMainClass(Resource<?> resource) {
    var mainPackage = getTopLevelPackage();
    var mainClass = "%sApplication".formatted(initUpper(applicationName()));
    var mainResource =
        resource
            .select("src/main/java")
            .select(mainPackage.replace(".", "/"))
            .select("%s.java".formatted(mainClass));
    if (mainResource.exists()) {
      return Optional.empty();
    }
    var code = MAIN_CLASS_CODE.formatted(mainPackage, mainClass);
    return Optional.of(
        new CodeArtifact(new FullyQualifiedName(mainPackage, mainClass)).setCode(code));
  }

  private Optional<CodeArtifact> ensureDomainServiceIsSpringBean(
      Aggregate aggregate, Resource<?> resource) {
    var serviceName = serviceNameFor(aggregate) + "Impl";
    var serviceFqn = serviceFqnFor(aggregate, serviceName);
    var serviceResource =
        resource.select("src/main/java").select("%s.java".formatted(serviceFqn.replace('.', '/')));
    if (!serviceResource.exists()) {
      log.error("Missing domain service for controller at {}", serviceResource.path());
      return Optional.empty();
    }
    var code = serviceResource.readAsString();
    if (code.contains("@Service")) {
      return Optional.empty();
    }
    var service = new CodeArtifact(new FullyQualifiedName(serviceFqn));
    code = code.replaceFirst("import", "import org.springframework.stereotype.Service;\n\nimport");
    code = code.replace("public class", "@Service\npublic class");
    return Optional.of(service.setCode(code));
  }

  private String serviceFqnFor(Aggregate aggregate, String serviceName) {
    return "%s.%s".formatted(packageFor(aggregate, "domain.services"), serviceName);
  }

  private String serviceNameFor(Aggregate aggregate) {
    return aggregate.getName() + "Service";
  }

  private CodeArtifact controllerFor(Aggregate aggregate, Command command, Event event) {
    var packageName = packageFor(aggregate, "inbound.http");
    var name = aggregate.getName() + "Controller";
    var serviceName = serviceNameFor(aggregate);
    var serviceFqn = serviceFqnFor(aggregate, serviceName);
    var commandFqn = "%s.%s".formatted(packageFor(command, "domain.model"), command.getName());
    var eventFqn = "%s.%s".formatted(packageFor(event, "domain.model"), event.getName());
    var imports =
        Stream.of(
                commandFqn,
                eventFqn,
                serviceFqn,
                "lombok.RequiredArgsConstructor",
                "org.springframework.web.bind.annotation.PostMapping",
                "org.springframework.web.bind.annotation.RequestBody",
                "org.springframework.web.bind.annotation.RestController")
            .sorted()
            .map("import %s;"::formatted)
            .collect(joining("\n"));
    var paramName = initLower(command.getName());
    var serviceFieldName = initLower(serviceName);
    var endpointUrl = "/%s".formatted(toKebabCase(command.getName()));
    var code =
        """
        package %s;

        %s

        @RestController
        @RequiredArgsConstructor
        public class %s {

          private final %s %s;

          @PostMapping("%s")
          public %s %s(@RequestBody %s %s) {
            return %s.accept(%s);
          }
        }
        """
            .formatted(
                packageName,
                imports,
                name,
                serviceName,
                serviceFieldName,
                endpointUrl,
                event.getName(),
                paramName,
                command.getName(),
                paramName,
                serviceFieldName,
                paramName);
    return codeArtifact(packageName, name, code);
  }
}
