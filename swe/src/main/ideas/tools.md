# Architecture

The architecture is mainly
[pipes and filters](https://www.enterpriseintegrationpatterns.com/patterns/messaging/PipesAndFilters.html):

- The filters are tools that consume inputs and produce outputs.
- The overall system implements the pipes that connect the outputs of one tool to the inputs of another.

## Tools infrastructure

If we maintain a registry of tools, we can reason about them in the abstract.
For instance, a Gradle plugin could read all tools from the registry and generate task definitions, without
understanding the specifics of any particular tool.
Or an IntelliJ plugin could read all input formats and register file extensions and syntax highlighters for them,
without understanding the specifics of particular file format.

```mermaid
graph
    Tool -- registered in --> ToolRegistry
    Tool -- defines\noutput as --> Output
    Output -- builds\nartifacts at --> Glob
    Tool -- defines input as --> Input
    Input -- finds\nartifacts\nusing --> Glob
    Input -- parses\nartifacts\nusing --> Format
    Format -- uses --> Parser
    Format -- uses --> Builder
    Parser -- parses --> Artifact
    Builder -- builds --> Artifact
    Tool -- may build\nartifacts using --> Builder
    Tool -- consumes --> Artifact
    Tool -- produces --> Artifact
    Tool -- issues --> Diagnostic
    Diagnostic -- provides --> Suggestion
```

Every tool would live in its own jar and depend on a shared jar that implements the above model and on jars that
implement parsers/builders for the artifact type that it uses/produces.

## Gradle

- `assemble` task depends on each tool's "build" functionality
- `check` task depends on each tool's "validation" functionality
- `help` task depends on each tool's "suggestions" functionality


## Tools & artifacts

```mermaid
graph
    classDef done fill:green,color:white
    classDef wip fill:yellow
    classDef todo fill:red,color:white
    
    DomainStoryTool([DomainStoryTool]);
    UseCaseTool([UseCaseTool])
    CommandTool([CommandTool])
    AggregateTool([AggregateTool])
    EventTool([EventTool])
    ReadModelTool([ReadModelTool])
    DomainTool([DomainTool])
    ModuleTool([ModuleTool])
    ComponentTool([ComponentTool])
    AcceptanceTestTool([AcceptanceTestTool])
    UnitTestTool([UnitTestTool])
    EntityTool([EntityTool])
    CiCdTool([CiCdTool])
    GlossaryTool([GlossaryTool])
    DecisionTool([DecisionTool])
    ArchitectureTool([ArchitectureTool])
    ScreenTool([ScreenTool])
    InfraTool([InfraTool])
    ProjectTool([ProjectTool])
    
    ProjectTool <--> Vision
    ProjectTool <--> BusinessRequirement
    BusinessRequirement <--> BusinessRequirementTool
    BusinessRequirementTool <--> UserRequirement
    UserRequirement <--> UserRequirementTool
    UserRequirementTool <--> DomainStory
    ProjectTool <--> Stakeholder
    Stakeholder --> DomainStoryTool
    Stakeholder --> UseCaseTool
    DomainStory <--> DomainStoryTool
    DomainStoryTool --> UseCase
    UseCase <--> UseCaseTool
    UseCaseTool --> Command
    Command <--> CommandTool
    CommandTool --> Entity
    UseCaseTool --> Aggregate
    Aggregate <--> AggregateTool
    UseCaseTool --> Event 
    Event <--> EventTool
    EventTool --> Entity
    AggregateTool --> Entity
    UseCaseTool --> ReadModel
    ReadModel <--> ReadModelTool
    ReadModelTool --> Entity
    Entity <--> EntityTool
    Decision --> EntityTool
    EntityTool <--> Schema
    UseCaseTool --> Policy
    UseCaseTool --> Screen
    Screen <--> ScreenTool
    Decision --> ScreenTool
    ScreenTool <--> Form
    UseCaseTool --> Domain
    Domain <--> DomainTool
    DomainTool --> Module
    Module <--> ModuleTool
    Decision --> ModuleTool
    ModuleTool --> Component
    Component <--> ComponentTool
    Decision --> ComponentTool
    ComponentTool <--> Containerfile
    UseCaseTool --> AcceptanceTest
    Decision --> AcceptanceTestTool
    AcceptanceTest <--> AcceptanceTestTool
    Aggregate --> AcceptanceTestTool
    ReadModel --> AcceptanceTestTool
    Policy --> AcceptanceTestTool
    AcceptanceTestTool <--> UnitTest
    UnitTest <--> UnitTestTool
    Decision --> UnitTestTool
    UnitTestTool <--> Code
    Component --> UnitTestTool
    Component --> CiCdTool
    Decision --> CiCdTool
    CiCdTool <--> CiCdPipeline
    DomainStoryTool --> Term
    Term <--> GlossaryTool
    UseCaseTool --> Term
    Decision <--> DecisionTool
    ArchitectureTool <--> Decision
    Decision --> InfraTool
    InfraTool <--> Iac

    class DomainStory done;
    class DomainStoryTool wip;
    class UseCase done;
    class UseCaseTool wip;
    class Command done;
    class CommandTool done;
    class Aggregate done;
    class AggregateTool todo;
    class Event done;
    class EventTool todo;
    class ReadModel done;
    class ReadModelTool todo;
    class Policy done;
    class Domain done;
    class DomainTool wip;
    class Module done;
    class AcceptanceTest done;
    class Entity done;
    class Decision todo;
    class EntityTool todo;
    class Schema todo;
    class ModuleTool wip;
    class Component done;
    class AcceptanceTestTool todo;
    class UnitTest todo;
    class UnitTestTool todo;
    class CiCdTool todo;
    class Code todo;
    class CiCdPipeline todo;
    class Term done;
    class GlossaryTool wip;
    class DecisionTool todo;
    class ComponentTool todo;
    class ArchitectureTool todo;
    class Screen todo;
    class ScreenTool todo;
    class Containerfile todo;
    class Form todo;
    class InfraTool todo;
    class Iac todo;
    class Stakeholder done;
    class ProjectTool wip;
    class Vision todo;
    class BusinessRequirement todo;
    class BusinessRequirementTool todo;
    class UserRequirement todo;
    class UserRequirementTool todo;
```
