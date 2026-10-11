# Bug 2920: NPE when no keyspaces are defined in cassandra.yaml

**Commit**: e70502c094 · **Ticket**: CASSANDRA-1080

## Detection Rule
If a configuration collection field can be null when no entries are specified in YAML but the code calls `.size()` on it without a null check, a NullPointerException occurs at startup.

## Code Smell
`conf.keyspaces.size() > 0` called without first checking `conf.keyspaces != null`; when the YAML file omits the `keyspaces` key, the field is null.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: unchecked-config-collection
- severity: crash
- subsystem: bootstrap-startup

## Root Cause
`DatabaseDescriptor` checked whether any keyspaces were defined in the YAML config to emit a warning. When the YAML contained no `keyspaces` section at all, the deserialized `conf.keyspaces` field was null, causing an NPE on `.size()`. Adding a null guard makes the code tolerate configs that omit the keyspaces section entirely.

## Example
```java
// BEFORE
if (conf.keyspaces.size() > 0)   // NPE if keyspaces omitted from yaml
    logger.warn("...");
```
```java
// AFTER
if (conf.keyspaces != null && conf.keyspaces.size() > 0)
    logger.warn("...");
```
