# Design

Given the requirements in event storming format, the designer must add information to commands, aggregates, events, and
readModels.
A big part of that is to define entities (in the DDD sense).

Again, the event storming grammar helps out.
Consider the sequence command -> aggregate -> event.
Each of these carries information modeled as an entity.
The fields in the command's entity plus the fields in the aggregate's entity are usually a superset of the fields in the
event's entity.
Additional information is captured in the command's type.

This suggests that a dedicated editor can help:

- For the event's field, select from the command's and aggregate's fields
- Warn about more than one field in the event's entity that isn't mapped to any field in the command's and aggregate's
  entities' fields

Similar considerations apply for the sequence event + read model -> policy -> command.
