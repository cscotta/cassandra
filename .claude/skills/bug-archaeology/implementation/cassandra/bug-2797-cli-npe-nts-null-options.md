# Bug 2797: CLI "show keyspaces" NPE When NetworkTopologyStrategy Has Null Options

**Commit**: 4254bcfb96 · **Ticket**: CASSANDRA-2049

## Detection Rule
When code passes a potentially null field directly to a formatting method (`FBUtilities.toString(map)`) without a null guard, the CLI crashes with `NullPointerException` instead of displaying an empty option list.

## Code Smell
`FBUtilities.toString(ks_def.strategy_options)` is called where `strategy_options` can be `null` for keyspaces that have a `NetworkTopologyStrategy` but no configured DC options (e.g., during migration or misconfiguration).

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: null-map-to-format-method
- severity: crash
- subsystem: tools-clients

## Root Cause
The CLI's `executeShowKeyspaces` method printed strategy options only for `NetworkTopologyStrategy` keyspaces by calling `FBUtilities.toString(ks_def.strategy_options)`. If `strategy_options` was `null` (valid per the Thrift schema), `FBUtilities.toString` threw a `NullPointerException`, crashing the `show keyspaces` command. The fix adds a null check: when `options == null` an empty string is used, otherwise the map is formatted normally.

## Example
```java
// BEFORE: NPE if strategy_options is null
if (ks_def.strategy_class.endsWith(".NetworkTopologyStrategy"))
    sessionState.out.println("    Options: " + FBUtilities.toString(ks_def.strategy_options));
```
```java
// AFTER: null-safe
if (ks_def.strategy_class.endsWith(".NetworkTopologyStrategy")) {
    Map<String, String> options = ks_def.strategy_options;
    sessionState.out.println("    Options: [" + ((options == null) ? "" : FBUtilities.toString(options)) + "]");
}
```
