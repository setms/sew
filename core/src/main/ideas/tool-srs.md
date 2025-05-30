# System requirements specification

## Build

- Generate SRS from vision, scopes, stakeholders, business requirements, user requirements, and use cases
- Generate use case UML diagram
- Generate event storm from use cases

## Validate

- There must be a vision (warning)
- There must be at least one scope
- Business requirement must be realized by at least one user requirement (warning)
- User requirement must realize existing business requirement (error)
- User requirement must be captured by at least one use case (warning)
- Use case must capture existing user requirement (error)
- Use case must have at least one acceptance test for each aggregate, automated policy, and read model (warning)

## Suggest

- Create vision
- Create scope
- Create business requirement

