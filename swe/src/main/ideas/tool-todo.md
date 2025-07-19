# Knowledge work

## Summary of Wikipedia

Knowledge work can be differentiated from other forms of work by its emphasis on "non-routine" problem-solving that
requires a combination of convergent and divergent thinking.
There are various definitions, with the most narrow being:

> The direct manipulation of symbols to create an original knowledge product, or to add obvious value to an existing one.

This type of work includes a complex combination of skill sets or _creative knowledge work (ckw) capacities_.
Creative knowledge workers use a combination of creative applications to perform their functions/roles in the knowledge
economy, including anticipatory imagination, problem-solving, problem seeking, and generating ideas and aesthetic
sensibilities.

The two modes of working range from individual to collaborative, where a worker might be doing either or both depending 
on the specific activity.

Typical knowledge workers must have some system at their disposal to create, process and enhance their own knowledge.
The practice of knowledge management (KM) evolved support knowledge workers with standard tools and processes.

Much knowledge work relies on the smooth navigation of unstructured processes and the elaboration of custom and one-off
procedures.
Adaptive Case Management (ACM) refers to the coordination of a _service request_ on behalf of a subject such as a
customer, a citizen, or an employee.
The tasks required by a case usually involve creating a case folder or container for all required artifacts.
Case management is highly collaborative, dynamic, and contextual in nature, with events driving a long-lived case-based
business process.

Knowledge workers bring benefits to organizations in a variety of ways. These include:

- analyzing data to establish relationships
- assessing input to evaluate complex or conflicting priorities
- identifying and understanding trends
- making connections
- understanding cause and effect
- ability to brainstorm, thinking broadly (divergent thinking)
- ability to drill down, creating more focus (convergent thinking)
- producing a new capability
- creating or modifying a strategy

The theory of Human Interaction Management asserts the following principles characterizing effective knowledge work:

- Build effective teams
- Communicate in a structured way
- Create, share and maintain knowledge
- Align your time with strategic goals
- Negotiate next steps as you work

Knowledge Management (KM) needs to convert internalized tacit knowledge into explicit knowledge to share it.
One strategy to KM involves actively managing knowledge (push strategy). In such an instance, individuals strive to
explicitly encode their knowledge into a shared knowledge repository, as well as retrieving knowledge they need that
other individuals have provided (_codification_).
Codification is document-centered, where knowledge is codified into documents which are stored in a KM system.

A typical KM system facilitates collaboration and sharing of information.
It usually supports custom workflows consisting of standard tasks.


## SEW

Software development is knowledge work, so a Software Engineering Workbench must be able to codify knowledge into
documents.
It must allow individuals to create new documents, and add information to existing documents.
It must also facilitate collaboration on those documents.
The standard tasks of KN are tools that operate on types of documents that are specific to software development. 

We could see a feature request as a case, so case management may be relevant as well.
The difference with traditional cases is that feature requests aren't standalone items that are separate from other
cases.
Rather, they're packages of work that flow through the system and build on top of what's already there.
Also, a single product may have multiple such cases in progress at the same time.
In other words, cases must integrate with other cases, but at the same time some level of isolation is needed or else
people would step on each other's toes.
This implies some form of version management.
It also means that it's important to have a good overview of all the things that are being worked on at any given point 
in time, in other words, project management.

Version management, project management, and collaboration management can be seen as domains that support the core domain
of software development.
This would imply a preference for third party software.
However, for a frictionless user experience, we'd want deep integration that hides external systems.

Perhaps the Simplest Thing That Could Possibly Work is a todo list that shows tasks with filters for the case container,
required skills, etc.
Each person can claim a task by launching the tool that supports it.
A todo item corresponds to a diagnostic reported by a tool, where the tool offers suggestions for resolving the matter.

Todo items are documents, but unlike other documents, they're transient.
Once the knowledge worker has completed the task, the todo item goes away.
There is no need for traceability or iterative refinement, as is the case with other documents.
Nevertheless, the todo item must be stored in the same KM system for user experience reasons.

Both `src/main` and `src/test` contain permanent documents, so todo items mustn't be mixed in there.
The appropriate location `src/todo`.
Each todo item is a separate document in that directory, to avoid contention.
