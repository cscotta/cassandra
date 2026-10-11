# Bug 2876: Missing break in CLI switch causes gc_grace_seconds to fall through

**Commit**: e2ffe2b0d0 · **Ticket**: CASSANDRA-1549

## Detection Rule
When a switch statement handles an enum of CfDef property names and each case calls a different setter, verify every case has a `break` to prevent fall-through into the `default` assert-false.

## Code Smell
The `GC_GRACE_SECONDS` case in `CliClient`'s property-setting switch block sets `cfDef.setGc_grace_seconds(...)` but is missing a `break`, so execution falls through to `default: assert(false)` and the operation fails with an assertion error.

## Tags
- scope: java
- category: logic-error
- code-pattern: missing-break-fall-through
- severity: crash
- subsystem: tools-clients

## Root Cause
A `break` statement was accidentally omitted after `cfDef.setGc_grace_seconds(Integer.parseInt(mValue))`, causing control to fall through to the `default` branch which contains `assert(false)`. With assertions enabled the node throws an `AssertionError`; with assertions disabled the setting silently does nothing beyond the setter call (since the default branch contains only the assert).

## Example
```java
// BEFORE — falls through to assert(false)
case GC_GRACE_SECONDS:
    cfDef.setGc_grace_seconds(Integer.parseInt(mValue));
    // missing break!
default:
    assert(false);
```
```java
// AFTER
case GC_GRACE_SECONDS:
    cfDef.setGc_grace_seconds(Integer.parseInt(mValue));
    break;
default:
    assert(false);
```
