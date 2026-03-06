package org.setms.swe.domain.model.sdlc.code.java;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.stream.Stream;
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
    codeBuilder.addBuildPlugin("org.springframework.boot", resource);
    var controllerPackage = packageFor(aggregate, "inbound.http");
    var controllerName = aggregate.getName() + "Controller";
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
                controllerPackage,
                imports,
                controllerName,
                serviceName,
                serviceFieldName,
                endpointUrl,
                event.getName(),
                paramName,
                command.getName(),
                paramName,
                serviceFieldName,
                paramName);
    return List.of(codeArtifact(controllerPackage, controllerName, code));
  }
}
