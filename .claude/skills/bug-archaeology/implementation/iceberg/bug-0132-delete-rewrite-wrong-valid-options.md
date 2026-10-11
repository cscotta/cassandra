# Bug 0132: Position delete rewrite option validation misses runner options

**Commit**: 23b5e1d295e0a89d8aeeb49483c2f863fcd9142b · **Ticket**: #15828

## Detection Rule

When a set of valid options is constructed by combining multiple sources (planner, runner, fixed list), verify each source appears exactly once and that no source is substituted for another.

## Code Smell

A `Set` initialized from `planner.validOptions()` is then populated with `planner.validOptions()` again via `addAll`, while `runner.validOptions()` is never consulted — the builder call `Sets.newHashSet(planner.validOptions())` and the `addAll(planner.validOptions())` on the next two lines are both pointing at the same source.

## Tags

- scope: universal
- category: wrong-filter-result
- code-pattern: copy-paste-wrong-source
- severity: silent-wrong-result
- subsystem: spark/actions/RewritePositionDeleteFilesSparkAction

## Root Cause

`validateAndInitOptions` builds the set of valid option keys by seeding a `HashSet` from `planner.validOptions()` and then calling `addAll` with both `VALID_OPTIONS` and `planner.validOptions()` again. The intended seed was `runner.validOptions()`, so any runner-specific options (e.g. `max-concurrent-file-group-rewrites`) were silently absent from the whitelist. Users specifying valid runner options would receive an incorrect "unknown option" error.

## Example

```java
// BEFORE
private void validateAndInitOptions() {
    Set<String> validOptions = Sets.newHashSet(planner.validOptions()); // wrong: should be runner
    validOptions.addAll(VALID_OPTIONS);
    validOptions.addAll(planner.validOptions());
    ...
}
```

```java
// AFTER
private void validateAndInitOptions() {
    Set<String> validOptions = Sets.newHashSet(runner.validOptions()); // correct source
    validOptions.addAll(VALID_OPTIONS);
    validOptions.addAll(planner.validOptions());
    ...
}
```
