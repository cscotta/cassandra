## Commit

57ae6d6706 — KAFKA-18695: Remove obsolete quorum=kraft and kip932 annotations from integration tests

## Ticket

KAFKA-18695

## Subsystem

Test infrastructure / integration tests

## Category

State not cleaned up

## Root cause

Integration tests throughout the `core` and `clients` modules were annotated with `@ClusterTest(clusterType = ClusterType.KRAFT)` (kraft-specific) and `@Tag("kip932")` / `isShareGroupTest=true` markers that were added when KRaft and share groups were experimental. Once KRaft became the only supported mode and share group tests were reorganised after PR #19542, these annotations became dead code. The stale annotations confused CI tooling that filtered tests by tag and caused test-selection overhead. Removing them ensures all tests run by default under the correct cluster type without tag-based exclusions.

## Before / After

```java
// Before — redundant explicit kraft annotation
@ClusterTest(quorum = "kraft", ...)
void testTopicCreation() { ... }

// After — default cluster type is already kraft
@ClusterTest(...)
void testTopicCreation() { ... }

// Before — obsolete kip932 tag
@Tag("kip932")
class ShareGroupTest { ... }

// After — tag removed
class ShareGroupTest { ... }
```

## How to spot

Search for `quorum = "kraft"` in `@ClusterTest` annotations after KRaft became the default cluster type. Any such explicit annotation is redundant and indicates the test was written before the default changed but never updated.

## Test

The affected test classes continue to pass after annotation removal, confirming the annotations were purely decorative.
