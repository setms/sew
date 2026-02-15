# Plan: Add BuildTool Abstraction

## Context

The SEW project recently added a BuildTool decision (in `Decision` artifacts) that allows users to choose between build tools like Gradle or Maven. Currently, the system validates that a build tool decision exists but doesn't use it to generate build configuration files.

This change adds a `BuildTool` abstraction that:
1. Follows the existing `UnitTestGenerator` pattern for technology-specific generators
2. Allows `TechnologyResolver` to provide a `BuildTool` instance based on decisions
3. Enables `CodeTool` to generate build configuration files (e.g., `build.gradle`, `settings.gradle`)
4. Maintains the established diagnostic → suggestion → apply workflow

The goal is to generate minimal, working build configurations that match the decided-upon build tool, enabling users to compile and run their generated code.

## Implementation Approach

### 1. Create BuildTool Interface

**File:** `swe/src/main/java/org/setms/swe/domain/model/sdlc/technology/BuildTool.java` (new)

Create a simple interface following the `UnitTestGenerator` pattern:

```java
package org.setms.swe.domain.model.sdlc.technology;

import java.util.List;
import org.setms.swe.domain.model.sdlc.code.CodeArtifact;
import org.setms.swe.inbound.tool.TechnologyResolverImpl;

/**
 * Validates and generates build configuration files.
 *
 * <p>Implementations are build-tool specific (e.g., Gradle, Maven). They know what configuration
 * files they need, validate whether they exist, and generate them when requested. The selection
 * of which implementation to use is handled by {@link TechnologyResolverImpl}.
 */
public interface BuildTool {
  /**
   * Check if build configuration exists and emit diagnostic if missing.
   *
   * @param resource the project root resource to check for configuration files
   * @param diagnostics where to add diagnostic if configuration is missing
   */
  void validate(Resource<?> resource, Collection<Diagnostic> diagnostics);

  /**
   * Apply a suggestion to generate build configuration files.
   *
   * @param suggestionCode the suggestion code from the diagnostic
   * @param resource the project root resource
   * @return applied suggestion result with created resources
   */
  AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource);
}
```

**Why validate() + initProject()?** Each BuildTool implementation knows what files it needs (e.g., `build.gradle`, `pom.xml`) and should own that knowledge completely, including validation and generation.

**Why does GradleBuildTool need projectName?** Build tools need a project name for configuration files (e.g., `rootProject.name` in `settings.gradle`). The project name is provided via constructor, similar to how `JavaUnitTestGenerator` receives `topLevelPackage`.

### 2. Create Project Artifact

**File:** `swe/src/main/java/org/setms/swe/domain/model/sdlc/project/Project.java` (new)

```java
package org.setms.swe.domain.model.sdlc.project;

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
public class Project extends Artifact {
  private String name;

  public Project(FullyQualifiedName fullyQualifiedName) {
    super(fullyQualifiedName);
  }
}
```

**File format:** Create `.project` file format (SAL-based)

**Example:** `src/main/project/MyProject.project`
```
project MyProject {
  name: "my-project"
}
```

**Why a Project artifact?** The project name is fundamental information that other artifacts depend on (TopLevelPackage, build tools). Starting with just `name`, this artifact can grow over time to include company, description, version, etc.

**Inputs registration:** Add `Inputs.projects()` method similar to `Inputs.decisions()`

### 3. Implement GradleBuildTool

**File:** `swe/src/main/java/org/setms/swe/domain/model/sdlc/code/java/GradleBuildTool.java` (new)

```java
@RequiredArgsConstructor
public class GradleBuildTool implements BuildTool {
  static final String GENERATE_BUILD_CONFIG = "gradle.generate.build.config";

  private final String projectName;

  @Override
  public void validate(Resource<?> resource, Collection<Diagnostic> diagnostics) {
    // Check for Gradle-specific files
    if (!fileExists(resource, "build.gradle") || !fileExists(resource, "settings.gradle")) {
      diagnostics.add(new Diagnostic(
          WARN,
          "Missing build configuration",
          null,
          new Suggestion(GENERATE_BUILD_CONFIG, "Generate build configuration files")));
    }
  }

  @Override
  public AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource) {
    if (!GENERATE_BUILD_CONFIG.equals(suggestionCode)) {
      return AppliedSuggestion.none();
    }

    try {
      // Generate build.gradle
      var buildGradleResource = resource.select("/").select("build.gradle");
      try (var output = buildGradleResource.writeTo()) {
        output.write(createBuildGradleContent().getBytes());
      }

      // Generate settings.gradle
      var settingsGradleResource = resource.select("/").select("settings.gradle");
      try (var output = settingsGradleResource.writeTo()) {
        output.write(("rootProject.name = '" + projectName + "'\n").getBytes());
      }

      return AppliedSuggestion.created(List.of(buildGradleResource, settingsGradleResource));
    } catch (IOException e) {
      return AppliedSuggestion.failedWith(e);
    }
  }

  private String createBuildGradleContent() {
    // Use km/build.gradle as template, simplified (no ANTLR, Spotless, Pitest)
    // Include Java plugin, JUnit 5, AssertJ, JQwik
  }

  private boolean fileExists(Resource<?> resource, String path) {
    var file = resource.select("/").select(path);
    if (file == null) return false;
    try (var ignored = file.readFrom()) {
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
```

**Build file content:**
- `build.gradle`: Java plugin, JUnit 5 + AssertJ + JQwik dependencies (matching what `JavaUnitTestGenerator` expects)
- `settings.gradle`: `rootProject.name = 'projectName'` using the project name from the `Project` artifact

**Reference:** Use `km/build.gradle` as a template, but simplified (no ANTLR, Spotless, Pitest).

### 4. Extend TechnologyResolver Interface

**File:** `swe/src/main/java/org/setms/swe/domain/model/sdlc/technology/TechnologyResolver.java`

Add a new method parallel to `unitTestGenerator()`:

```java
/**
 * @param decisions Decisions made
 * @param inputs Resolved inputs (to access Project artifact)
 * @param resource Project root resource (passed to BuildTool.validate())
 * @param location From where the build tool is configured
 * @param diagnostics where to store any validation issues
 * @return something that can validate and generate build configuration, or empty if there are issues
 */
Optional<BuildTool> buildTool(
    Collection<Decision> decisions,
    ResolvedInputs inputs,
    Resource<?> resource,
    Location location,
    Collection<Diagnostic> diagnostics);
```

### 5. Implement Resolution Logic in TechnologyResolverImpl

**File:** `swe/src/main/java/org/setms/swe/inbound/tool/TechnologyResolverImpl.java`

Add `buildTool()` implementation:

```java
@Override
public Optional<BuildTool> buildTool(
    Collection<Decision> decisions,
    Resource<?> resource,
    Location location,
    Collection<Diagnostic> diagnostics) {
  var grouped = groupByTopic(decisions);
  var programmingLanguage = grouped.get(ProgrammingLanguage.TOPIC);
  var buildTool = grouped.get(BuildToolTopic.TOPIC);

  // Get project name from Project artifact (inputs should include Project.class)
  // var project = inputs.get(Project.class).stream().findFirst();
  // var projectName = project.map(Project::getName).orElse(null);

  // Emit diagnostics for missing decisions (following existing pattern)
  // If all present and Java + Gradle → create instance
  Optional<BuildTool> result = Optional.of(new GradleBuildTool(projectName));

  // Let the BuildTool validate itself
  result.ifPresent(bt -> bt.validate(resource, diagnostics));

  return result;
}
```

**Artifact chain:** Requires `Project` artifact with `name` property. The `TechnologyResolver` will need access to resolved inputs to get the `Project` artifact.

Handle `GradleBuildTool.GENERATE_BUILD_CONFIG` in `applySuggestion()`:

```java
@Override
public AppliedSuggestion applySuggestion(String suggestionCode, Resource<?> resource) {
  return switch (suggestionCode) {
    case PICK_PROGRAMMING_LANGUAGE -> pickDecision(...);
    case PICK_TOP_LEVEL_PACKAGE -> pickDecision(...);
    case PICK_BUILD_TOOL -> pickDecision(...);
    case GradleBuildTool.GENERATE_BUILD_CONFIG -> generateBuildConfig(resource);
    default -> AppliedSuggestion.none();
  };
}

private AppliedSuggestion generateBuildConfig(Resource<?> resource) {
  // Get BuildTool instance via buildTool(decisions, inputs, resource, ...)
  // Call buildTool.applySuggestion(suggestionCode, resource)
  // BuildTool handles file generation and returns AppliedSuggestion
}
```

### 6. Update CodeTool to Delegate to BuildTool

**File:** `swe/src/main/java/org/setms/swe/inbound/tool/CodeTool.java`

In `validate()` method, after checking for BuildTool decision:

```java
if (buildToolDecision is present) {
  // Delegate validation to BuildTool - it knows what files it needs
  technologyResolver.buildTool(decisions, inputs, codeArtifact.resource(), null, diagnostics);
}
```

**Key change:** CodeTool no longer checks for specific files like `build.gradle`. Instead, it delegates to the BuildTool implementation, which knows what configuration files it requires.

**Inputs update:** CodeTool's `validationContext()` must include `Inputs.projects()` so that the `Project` artifact is available for BuildTool.

In `applySuggestion()`, handle BuildTool suggestions:

```java
case GradleBuildTool.GENERATE_BUILD_CONFIG -> technologyResolver.applySuggestion(suggestionCode, resource);
// Future: case MavenBuildTool.GENERATE_BUILD_CONFIG -> ...
```

### 7. Update E2E Test - Iteration 08

**File:** `swe/src/test/resources/e2e/08/iteration.yaml`

Update the expected diagnostics to include the new build configuration diagnostic:

```yaml
diagnostics:
  - file: 'src/test/java/MyServiceTest.java'
    message: 'Missing build configuration'
    suggestions:
      - description: 'Generate build configuration files'
```

### 8. Add E2E Test - Iteration 09 (new)

**Directory:** `swe/src/test/resources/e2e/09/`

Create `iteration.yaml`:
```yaml
outputs:
  - 'build.gradle'
  - 'settings.gradle'
inputs: []
diagnostics: []
```

Create expected output files:
- `09/outputs/build.gradle` - Gradle build script with Java plugin and test dependencies
- `09/outputs/settings.gradle` - Simple settings file with project name

This iteration verifies that applying the "Generate build configuration files" suggestion creates the expected files, and that no new diagnostics are emitted afterward.

## Critical Files to Modify

- `swe/src/main/java/org/setms/swe/domain/model/sdlc/project/Project.java` (new artifact with `name` property)
- `swe/src/main/java/org/setms/swe/inbound/tool/Inputs.java` (add `projects()` method)
- `swe/src/main/java/org/setms/swe/domain/model/sdlc/technology/BuildTool.java` (new interface with validate() + initProject())
- `swe/src/main/java/org/setms/swe/domain/model/sdlc/code/java/GradleBuildTool.java` (new implementation taking projectName via constructor)
- `swe/src/main/java/org/setms/swe/domain/model/sdlc/technology/TechnologyResolver.java` (add buildTool() method with inputs parameter)
- `swe/src/main/java/org/setms/swe/inbound/tool/TechnologyResolverImpl.java` (implement buildTool() extracting projectName from Project artifact)
- `swe/src/main/java/org/setms/swe/inbound/tool/CodeTool.java` (add projects() to validationContext, delegate to buildTool.validate())
- `swe/src/test/resources/e2e/08/iteration.yaml` (add Project artifact input, update diagnostics)
- `swe/src/test/resources/e2e/09/` (new iteration directory + files)

## Reusable Components

**Existing patterns to follow:**
- `ArtifactTool` pattern - Tools own validation and diagnostics (see `CodeTool.validate()`, `UseCaseTool.validate()`)
- `UnitTestGenerator` interface → `BuildTool` interface pattern (but BuildTool also validates)
- `JavaUnitTestGenerator` implementation → `GradleBuildTool` implementation pattern
- `TechnologyResolverImpl.unitTestGenerator()` → `buildTool()` resolution pattern
- Suggestion code handling - Each tool defines its own codes (e.g., `GradleBuildTool.GENERATE_BUILD_CONFIG`)
- `Resource.select() + readFrom()` - Standard pattern for checking file existence (see `FileResource:84-88`)

**Existing utilities:**
- `km/build.gradle` as a template for what dependencies/plugins to include
- `CodeArtifact` for representing generated files
- `FullyQualifiedName` for artifact naming

## Verification

### Unit Tests (write before implementation)

1. **ProjectTest** (new):
    - `shouldHaveNameProperty()` - verify Project artifact has name property
    - `shouldSerializeAndDeserialize()` - verify SAL format works for Project

2. **GradleBuildToolTest** (new):
    - `shouldEmitDiagnosticWhenBuildGradleMissing()` - verify diagnostic when build.gradle doesn't exist
    - `shouldEmitDiagnosticWhenSettingsGradleMissing()` - verify diagnostic when settings.gradle doesn't exist
    - `shouldNotEmitDiagnosticWhenConfigurationExists()` - verify no diagnostic when both files exist
    - `shouldGenerateBuildGradleAndSettings()` - verify applySuggestion() generates 2 files
    - `shouldUseProjectNameInSettings()` - verify settings.gradle uses projectName from constructor (e.g., `"my-project"` → `rootProject.name = 'my-project'`)
    - `shouldIncludeRequiredDependencies()` - verify JUnit 5, AssertJ, JQwik in build.gradle

3. **TechnologyResolverImplTest** (extend):
    - `shouldReturnGradleBuildToolWhenDecided()` - verify resolution with BuildTool decision and Project artifact
    - `shouldPassProjectNameToGradleBuildTool()` - verify GradleBuildTool receives projectName from Project.name
    - `shouldCallBuildToolValidate()` - verify that buildTool() calls validate() on the instance
    - `shouldRequireProgrammingLanguageForBuildTool()` - verify diagnostic when missing
    - `shouldRequireProjectForBuildTool()` - verify diagnostic when Project artifact missing
    - `shouldGenerateBuildConfigurationWhenSuggestionApplied()` - verify GENERATE_BUILD_CONFIG creates files

4. **CodeToolTest** (extend):
    - `shouldIncludeProjectsInValidationContext()` - verify projects() added to validationContext
    - Remove hardcoded build.gradle checks
    - Verify CodeTool delegates to technologyResolver.buildTool()
    - Verify BuildTool decision diagnostic still works

### End-to-End Test

Run `EndToEndTest.shouldGuideSoftwareEngineering()`:
- Iteration 08: Should now expect "Missing build configuration" diagnostic (emitted by GradleBuildTool.validate())
- Iteration 09 (new): Should verify build files are generated (via GradleBuildTool.applySuggestion()) and no new diagnostics appear

### Benefits of This Architecture

1. **Complete encapsulation** - BuildTool owns all knowledge about its configuration files. GradleBuildTool knows about `build.gradle` and `settings.gradle`; a future MavenBuildTool would know about `pom.xml`.

2. **Extensibility** - Adding Maven support requires:
   - Implement `MavenBuildTool` with its own `validate()` checking for `pom.xml`
   - Add case in `TechnologyResolverImpl.applySuggestion()` for `MavenBuildTool.GENERATE_BUILD_CONFIG`
   - **No changes to CodeTool!**

3. **Separation of concerns** - CodeTool orchestrates tool invocation but doesn't know build-tool specifics.

4. **Follows existing patterns** - Mirrors how ArtifactTool implementations own their validation logic.

### Manual Verification

After implementation, the files in `swe/build/e2e/` should include:
- `build.gradle` with Java plugin and test dependencies
- `settings.gradle` with project name

You should be able to run `./gradlew build` in that directory and have it compile successfully (once Java files are generated).
