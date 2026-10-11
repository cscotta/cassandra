# Bug 1831: Wrong Parameter Name in TokenMetadata.pendingEndpointsFor After Merge

**Commit**: f15681b679 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/locator
**Category**: Logic error in condition

---

## Root Cause

A merge conflict left a parameter name inconsistency in `TokenMetadata.pendingEndpointsFor`: the method signature declared the parameter as `table` but the body referenced `keyspaceName`. Because Java resolves `keyspaceName` as the field or an outer-scope variable rather than the parameter, the method silently used a stale or wrong value instead of the argument passed by the caller. The fix renames the parameter from `table` to `keyspaceName` to match what the method body actually references.

### Before / After

```java
// BEFORE
public Collection<InetAddress> pendingEndpointsFor(Token token, String table)
{
    Map<Range<Token>, Collection<InetAddress>> ranges = getPendingRanges(keyspaceName);
```

```java
// AFTER
public Collection<InetAddress> pendingEndpointsFor(Token token, String keyspaceName)
{
    Map<Range<Token>, Collection<InetAddress>> ranges = getPendingRanges(keyspaceName);
```

---

## How to Spot

A method parameter that is declared but never referenced inside the method body (while the body references a same-concept variable by a different name) is a strong signal of a merge artifact or rename inconsistency. Static analysis tools that flag unused parameters would catch this immediately.

---

## Test

No test added.
