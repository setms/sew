# TDD

## Test list

- [x] `CodeBuilder` needs a method `addBuildPlugin(String)` to add a plugin to the build.
  `Gradle`'s implementation adds it to the version catalog and to `build.gradle`.
  It gets the latest version of the plugin from `plugins.gradle.org`.
- [x] `TechnologyResolverImpl.frameworkCodeGenerator` needs to pass the `CodeBuilder` to the `SpringBootCodeGenerator`
  constructor.
- [x] `SpringBootCodeGenerator.generateControllerFor` needs to add Spring Boot to the classpath by calling
  `CodeBuilder.addBuildPlugin("org.springframework.boot")`.
