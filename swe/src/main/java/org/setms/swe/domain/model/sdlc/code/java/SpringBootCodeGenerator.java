package org.setms.swe.domain.model.sdlc.code.java;

import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.format.Strings.initUpper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.setms.km.domain.model.artifact.FullyQualifiedName;
import org.setms.km.domain.model.format.Strings;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.domain.model.sdlc.design.Entity;
import org.setms.swe.domain.model.sdlc.eventstorming.Aggregate;
import org.setms.swe.domain.model.sdlc.eventstorming.Command;
import org.setms.swe.domain.model.sdlc.eventstorming.Event;
import org.setms.swe.domain.model.sdlc.technology.CodeBuilder;
import org.setms.swe.domain.model.sdlc.technology.FrameworkCodeGenerator;

public class SpringBootCodeGenerator extends JavaBaseCodeGenerator
    implements FrameworkCodeGenerator {

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

  public SpringBootCodeGenerator(String topLevelPackage, CodeBuilder codeBuilder) {
    super(topLevelPackage);
    this.codeBuilder = codeBuilder;
  }

  @Override
  public List<CodeArtifact> generateControllerFor(
      Resource<?> resource,
      Aggregate aggregate,
      Command command,
      Entity commandPayload,
      Event event) {
    ensureSpringBootWebDependency(resource);
    var result = new ArrayList<CodeArtifact>();
    ensureMainClass(resource).ifPresent(result::add);
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
    var mainClass =
        "%sApplication"
            .formatted(initUpper(mainPackage.substring(mainPackage.lastIndexOf('.') + 1)));
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

  private CodeArtifact controllerFor(Aggregate aggregate, Command command, Event event) {
    var packageName = packageFor(aggregate, "inbound.http");
    var name = aggregate.getName() + "Controller";
    var serviceName = aggregate.getName() + "Service";
    var serviceFqn = "%s.%s".formatted(packageFor(aggregate, "domain.services"), serviceName);
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
    var paramName = Strings.initLower(command.getName());
    var serviceFieldName = Strings.initLower(serviceName);
    var endpointUrl = "/%s".formatted(Strings.initLower(aggregate.getName()));
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
