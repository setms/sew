# Tool infrastructure

If we maintain a registry of tools, we can reason about them in the abstract.
For instance, a Gradle plugin could read all tools from the registry and generate task definitions, without
understanding any specifics about any particular tool.
Or an IntelliJ plugin could read all artifact types and register syntax highlighters/parsers for them, without
understanding any specifics about any particular artifact type or file format.

```mermaid
graph
    ArtifactType -- registered in --> Registry
    Tool -- registered in --> Registry
    Tool -- defines input as --> Input
    Input -- filters artifacts by --> Glob
    Input -- consumes artifacts of type --> ArtifactType
    ArtifactType -- defined by --> Format
    Format -- uses --> Builder
    Format -- uses --> Parser
    Parser -- parses --> Artifact
    Builder -- builds --> Artifact
    Tool -- defines output as --> Output
    Output -- builds artifacts of type --> ArtifactType
    Tool -- consumes --> Artifact
    Tool -- produces --> Artifact
    Tool -- issues --> Diagnostic
```

Every tool would live in its own jar and depend on a shared jar that implements the above model and on jars that
implement parsers/builders for the artifact type that it uses/produces.
