**Commit**: 006af8b939 · **Ticket**: KAFKA-17327
**Subsystem**: tools/ConfigCommand
**Category**: Missing null/bounds check

**Root cause**
`kafka-configs.sh` / `ConfigCommand` did not support `--group` or `--entity-type groups` for `--alter` and `--describe` operations. Users trying to describe or alter consumer-group-level configs via the CLI received an unhelpful error or had to use the Admin API directly. The fix adds GROUP as a recognised entity type in `ConfigCommand`, wires it through the option parser, and delegates to the appropriate Admin API calls for group-level config operations.

**Before/After**
```java
// Before: GROUP not handled in alter/describe dispatch
switch (entityType) {
    case TOPIC: ...
    case BROKER: ...
    // GROUP missing — fell through to error
}

// After
case GROUP:
    alterGroupConfigs(adminClient, opts);
    break;
```

**How to spot**
When adding a new entity type to an admin CLI, verify that every command (alter, describe, delete) has a handler for the new type. A missing case in a switch/if-chain causes all operations on that entity type to fail with a confusing error.

**Test**
`ConfigCommandTest` updated with cases for `--entity-type groups` alter and describe operations.
