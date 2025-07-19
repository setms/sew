# Generating candidates for domains from an event storm

> From a raw event storm, how can I infer likely subdomain boundaries and their strategic classification (core/supporting/generic)?

Here’s a stripped-down, model-first approach.

## 🧭 Phase 1: Partitioning the Storm into Candidate Subdomains

Before classifying anything as core/supporting/generic, you need to partition the storm into cohesive clusters. These will become candidate subdomains.

### Heuristics for Clustering

1. Event Flow Cohesion 
   Look for areas where:

   - Events lead to commands via policies.
   - Aggregates produce events consumed only within a local area.
   - Policies form loops with events and commands.

   👉 Treat these self-contained clusters as bounded context candidates.

1. Sticky Gravity

   - If a group of aggregates, commands, and events consistently interact with each other but not with the rest of the board, they likely belong together.

1. Name Proximity

   - Domain terms repeating across commands/events in a localized area (e.g., PolicyCreated, PolicyCancelled, PolicyRenewed) are a strong hint of a natural subdomain.

Once you've done that, you’ll have a handful of clusters that act like proto-subdomains. You can now move to classification.

## 🧮 Phase 2: Scoring Inferred Subdomains

Now that you’ve got clusters, you can apply structural heuristics that don’t rely on internal logic.

Here’s an updated checklist that requires no inside knowledge, only stickies and their connections:

| Criteria                 | Observation                                                                       | Points |
|--------------------------|-----------------------------------------------------------------------------------|--------|
| Internal Event Loop      | Subdomain has ≥1 policy that listens to its own events and triggers new commands. | +2     |
| Fan-out                  | Events from this cluster are consumed by policies outside the cluster.            | +2     |
| Fan-in                   | Policies in this cluster react to events from outside clusters.                   | -1     |
| Local Vocabulary Density | Many terms (nouns/verbs) are specific and cohesive within this cluster.           | +1     |
| Vocabulary Generality    | Terms are generic (e.g., User, Email, Notification, Audit).                       | -1     |
| Event Centrality         | Events from this cluster appear in many places across the board.                  | +1     |
| Stickies per Cluster     | 10+ elements (commands, events, aggregates, policies) in this group.              | +1     |
| Isolated Utility         | Subdomain is triggered by others but never triggers them.                         | -2     |

You can observe all of these from the storm itself without any additional artifacts.

### 📊 Classification Heuristic

| Score | Inferred Type        |
|-------|----------------------|
| ≥ 5   | Core candidate       |
| 2–4   | Supporting candidate |
| ≤ 1   | Generic              |


## ⚠️ Limitations (But Still Useful)

- This is not definitive. You’re not discovering truth — you’re creating an initial hypothesis for strategic DDD refinement.
- Once you've identified clusters and rough classification, bring in business and product people to validate.
- These insights can also be used to propose bounded context boundaries for further modeling.

