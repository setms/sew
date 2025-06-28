# Components

Start with modules/subdomain, then group into components.

## Dark energy and dark matter

Chris Richardson uses the metaphor of dark energy (repulsive force) and dark matter (attractive force).

Dark energy:

- **Simple components**: smaller is better
  - larger subdomains push away other subdomains more than smaller subdomains do
- **Team autonomy**: deploy team's services independently of others
  - assign subdomains to teams
  - loose coupling between teams
- **Fast deployment pipeline**: component must be fast to build and test
  - estimate the number of operations to build and test the event storm elements in the subdomain
  - exponentially penalize when the number of operations is over a threshold
- **Support multiple tech stacks**: polyglot components
  - pick technologies for subdomains, e.g. python when heavy on ML
  - organize compatible technologies into tech stacks, e.g. [ java, spring boot, graphql ]
  - subdomains with different tech stacks go into different components
- **Segregate by characteristics**: different components are scaled/secured/etc differently
  - applicable regulations (HIPAA, PCI-DCS)
  - resource requirements (CPU, GPU, RAM, network bandwidth)
  - business criticality (SLA)
  - core/supporting/generic (how is this different from the previous?)
  - security/availability/scalability/etc

Dark matter:

- **Simple interaction**: local is easier than distributed
  - An operation’s complexity is proportional to the number of components that collaborate to implement it and the
    number of messages they exchange
  - group the subdomains so that the critical operations are as simple as possible
- **Efficient interaction**: local is faster than distributed
  - synchronous operations between components are expensive
  - latency is proportional to # messages * message size
- **Prefer ACID to BASE**: eventual consistency is a pain
  - BASE lacks I (isolation), and is therefore hard to get right
  - Do we need operations with compensating transactions? Then merging components is better
- **Minimize runtime coupling**: availability of a component depends on the availability of the components it depends on
  - Operations that span components should be avoided 
- **Minimize design time coupling**: more or less the same as _simple interaction_ but on team level
  - Dependencies between modules become dependencies between the components that contain those modules
- **Organizational unpreparedness**: you must be this tall to use µservices
  - Need better design skills
  - Benefits from DevOps and Team Topologies

What are "operations" in the above?

- "System operations are invoked by external actors"
- Maybe they're scenarios?
  - No, one scenario can be multiple operations
    - e.g. "Nonuser requests data deletion": two operations, cuz two users issuing commands
- But that assumes you're not splitting scenarios for readability (xref Gdpr/DeleteData.useCase)
- Maybe merge all scenarios, then break up by actors (users, external systems)? 
- Is a scenario description list a list of operations translated into natural language?
  - Probably not, since an actor doing something and the system responding should be different lines
  - But maybe an operation is always exactly: an actor doing something and the system responding?


## Service integrators and disintegrators

In _Software Architecture, the Hard Parts_, Ford et al use a similar approach to dark energy/matter.

Service disintegrators == dark energy:

- **Scope**: prefer a single-purpose services with high cohesion
- **Volatility**: agility comes from reduces test scope and deployment risk
- **Scalability**: separating services with different scaling requirements leads to lower overall cost and faster
  responses
- **Fault tolerance**: each service can continue to provide value, even if another service is down
- **Security**: only grant access to required functionality

Service integrators == dark matter:

- **Database transactions**: it's easier to give guarantees if all parties in a transaction are together
- **Workflow**: distributed communication is a pain, local is easy
- **Shared code**: easier to upgrade in monolith than in n µservices
- **Data relationships**: separating data is a pain when you need it together, distributed calls to get it are
  expensive and fragile 


## Residuality

Barry O'Reilly introduces Residuality Theory (RT), where stressors are applied to a system to see what survives
(residues).

Some of these stressors are already accounted for by the above attractive and repulsive forces.
These affect how modules are grouped into components.
Let's call these _structural stressors_.

_TODO: merge categories from the two sections above to identify and properly name structural stressors._

Other stressors require new features for the system to survive.
Let's call these _behavioral stressors_.
This requires going back to the requirements.
We can do that by navigating from components to the modules they contain, from modules to the subdomains they implement,
and finally from subdomains to event storms that use elements from the subdomains.

We would then have to re-run the process from modified event storm to subdomains to modules to components.
We do that for a number of stressors and observe the resulting components for each.
Then we somehow merge all these component sets into one that we're going to implement.

_TODO: Figure this part out. How does the [incidence matrix](https://en.wikipedia.org/wiki/Incidence_matrix) fit in?_

Note that we can limit ourselves to components that contain modules that implement _core_ subdomains.
Supporting and generic subdomains aren't critical to the business, so the impact of changes to them is likely not
threatening the company's survival and therefore don't require the additional analysis.

_TODO: Figure out how to determine behavioral stressors._

This cycle from requirements -> architecture -> requirements poses the question of how the architect can influence
requirements, in particular where they result in different business processes.
For example, the license plate scanning feature O'Reilly introduces in his EV charging example would lead to a new
business process for procuring access to a license plate database.
Also, the SMEs that usually drive requirements are unlikely to want to go off in a bunch of different directions to
work through scenarios to deal with stressors they see as unlikely.
One way out of this would be to add the stressors to a risk registry if that's a thing in the organization.
