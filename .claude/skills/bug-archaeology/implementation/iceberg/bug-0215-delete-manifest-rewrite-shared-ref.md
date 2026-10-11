# Bug 0215: RewriteTablePathUtil adds original delete file reference instead of copy to toRewrite list

**Commit**: e822643ef06e8164c5d2314321b1278620b2ff97 · **Ticket**: #15155

## Detection Rule

When collecting objects into a results list that will be mutated or moved by subsequent processing, verify that a defensive copy is added rather than the original reference, especially when the same object is also stored in a separate mapping structure.

## Code Smell

`result.toRewrite().add(file)` where `file` is the same mutable object whose location is also stored in a sibling path-mapping list; later processing that rewrites only the first entry silently leaves all others referencing the original path.

## Tags

- scope: universal
- category: logic-error
- code-pattern: shared-mutable-reference
- severity: silent-wrong-result
- subsystem: core/RewriteTablePathUtil

## Root Cause

In the `POSITION_DELETES` branch of `rewriteDeleteManifest`, the code correctly built a new `posDeleteFile` with the rewritten path and stored it for the path mapping, but then added the original `file` reference (not its copy) to the `toRewrite` list. Only the first entry from the iterator was ever rewritten while subsequent delete files retained stale source paths, making the rewrite result incomplete for manifests containing multiple position delete entries.

## Example

```java
// BEFORE
result.toRewrite().add(file);   // original reference — stale path for entries 2..N
```

```java
// AFTER
result.toRewrite().add(file.copy());  // defensive copy with correct path
```
