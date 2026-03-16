# TDD

## Test list

- [x] `AggregateTool.validate()` should warn about missing domain object for the aggregate, just like it does for the
  domain service.
  This diagnostic should contain a suggestion to create the missing domain object.
- [~] `AggregateTool.doApply()` should handle the suggestion to create a missing domain object.
