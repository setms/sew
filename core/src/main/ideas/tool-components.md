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
  - organize compatible technologies into tech stacks, e.g. java, spring boot, graphql
  - subdomains with different tech stacks go into different components
- **Segregate by characteristics**: different components are scaled/secured/etc differently
  - applicable regulations (HIPAA, PCI-DCS)
  - resource requirements (CPU, GPU, RAM, network bandwidth)
  - business criticality (SLA)
  - core/supporting/generic
  - security/availability/scalability/etc

Dark matter:

- **Simple interaction**: local is easier than distributed
- **Efficient interaction**: local is faster than distributed
- **Prefer ACID over BASE**: eventual consistency is a pain
- **Minimize runtime coupling**: more or less the same as _efficient interaction_ 
- **Minimize design time coupling**: more or less the same as _simple interaction_ but on team level