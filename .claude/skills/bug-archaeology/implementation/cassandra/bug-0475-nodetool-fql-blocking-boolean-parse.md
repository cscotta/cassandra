# Bug 0475: nodetool enablefullquerylog --blocking Parameter Not Parsed as Boolean

**Commit**: 94c52450ed · **Ticket**: CASSANDRA-15819
**Subsystem**: `src/java/org/apache/cassandra/tools/nodetool/EnableFullQueryLog.java`
**Category**: Logic error in condition

---

## Root Cause

The `--blocking` CLI option was declared as a `Boolean` (boxed) type with JCommander. JCommander parses boolean options as flags — passing `--blocking true` would set the field to `true` and treat the literal string `"true"` as a positional argument, not a value, causing a parse error. The field needed to be `String` so JCommander treats the following token as the option's value, with manual conversion to `Boolean` afterward.

### Before / After

```java
// BEFORE
@Option(title = "blocking", name = {"--blocking"},
        description = "If the queue is full whether to block producers or drop samples.")
private Boolean blocking = null;
// ...
probe.enableFullQueryLogger(path, rollCycle, blocking, ...);
```

```java
// AFTER
@Option(title = "blocking", name = {"--blocking"},
        description = "... [true|false].")
private String blocking = null;
// ...
Boolean bblocking = null;
if (blocking != null) {
    if (!blocking.equalsIgnoreCase("TRUE") && !blocking.equalsIgnoreCase("FALSE"))
        throw new IllegalArgumentException("Invalid [" + blocking + "]. ...");
    else
        bblocking = Boolean.parseBoolean(blocking);
}
probe.enableFullQueryLogger(path, rollCycle, bblocking, ...);
```

---

## How to Spot

JCommander (and most CLI parsing libraries) treat boxed `Boolean` / `boolean` fields as flags — the option name alone sets it to `true` and no value token is consumed. Whenever a nodetool option needs an explicit `true`/`false` argument rather than a presence flag, the field type must be `String` with manual validation. Review CLI options where `Boolean` is used as a field type and an explicit value argument is expected.

---

## Test

No test added.
