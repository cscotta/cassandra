# Bug 1382: cassandra-stress Uses Mixed-Case Table and Keyspace Names

**Commit**: f0767fdf49 · **Ticket**: n/a
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/settings/Command.java
**Category**: Wrong constant / default value

---

## Root Cause

The default table names used by cassandra-stress (`Standard1`, `Counter1`) and the default keyspace name (`Keyspace1`) contained uppercase letters. Cassandra normalizes unquoted identifiers to lowercase, so a stress run would create tables named `standard1`, `counter1`, and keyspace `keyspace1` at the server level, while the stress tool internally referenced `Standard1`, `Counter1`, and `Keyspace1`. On case-sensitive comparisons or when schema metadata was inspected, this mismatch caused the tool to fail to locate the tables it had just created, or to create duplicate tables with mismatched names.

### Before / After

```java
// BEFORE: mixed-case defaults that diverge from Cassandra's lowercase normalization
READ(false, "Standard1", "Super1", ...),
WRITE(true, "Standard1", "Super1", ...),
COUNTER_WRITE(true, "Counter1", "SuperCounter1", ...),
final OptionSimple keyspace = new OptionSimple("keyspace=", ".*", "Keyspace1", ...);
```

```java
// AFTER: all-lowercase defaults that match Cassandra's identifier normalization
READ(false, "standard1", "Super1", ...),
WRITE(true, "standard1", "Super1", ...),
COUNTER_WRITE(true, "counter1", "SuperCounter1", ...),
final OptionSimple keyspace = new OptionSimple("keyspace=", ".*", "keyspace1", ...);
```

---

## How to Spot

Any place in a Cassandra client or tool where a CQL identifier is stored as a string literal should use the lowercase form unless the identifier is intentionally quoted. A reviewer should verify that hardcoded schema names round-trip through `toLowerCase()` unchanged; if they don't, the tool will silently disagree with the server about what object it is operating on.

---

## Test

No test added.
