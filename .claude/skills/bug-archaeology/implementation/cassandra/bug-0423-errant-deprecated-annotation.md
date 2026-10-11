# Bug 0423: @Deprecated Annotation on Wrong StorageService JMX Methods

**Commit**: 6e27e8bd4c · **Ticket**: n/a
**Subsystem**: service (StorageService JMX API)
**Category**: Wrong constant / default value

---

## Root Cause

Several pairs of `StorageService` methods exist in both a legacy (no-port) and a current (with-port) form: `getJoiningNodes`/`getJoiningNodesWithPort`, `getLiveNodes`/`getLiveNodesWithPort`, etc.  The `@Deprecated` annotation was placed on the wrong member of each pair — it annotated the `WithPort` variant (the intended replacement) rather than the plain variant (the one that should be replaced).  This caused IDEs, static analysis tools, and downstream callers to treat the current API as deprecated and the legacy API as supported, the exact opposite of the desired contract.

### Before / After

```java
// BEFORE – @Deprecated on the wrong method
public List<String> getJoiningNodes()          { ... }  // no annotation — appears current

@Deprecated
public List<String> getJoiningNodesWithPort()  { ... }  // annotated — appears deprecated
```

```java
// AFTER – @Deprecated on the legacy no-port method
@Deprecated
public List<String> getJoiningNodes()          { ... }

public List<String> getJoiningNodesWithPort()  { ... }  // no annotation — appears current
```

---

## How to Spot

When a codebase introduces `WithPort` (or `V2`, `New`, etc.) variants to supersede existing methods, verify that `@Deprecated` marks the old variants, not the new ones.  A quick way to catch this during review is to check that every `@Deprecated` method has a Javadoc `@deprecated` tag pointing to its replacement; if the tag says "use X instead" but X is the annotated method itself, the annotation is on the wrong method.

---

## Test

No test added.
