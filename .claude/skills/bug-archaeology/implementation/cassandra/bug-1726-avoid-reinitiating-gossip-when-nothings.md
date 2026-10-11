# Bug 1726: Reference Equality Used for String Comparison in GossipingPropertyFileSnitch

**Commit**: 89bccb6f7b · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/locator/GossipingPropertyFileSnitch.java
**Category**: Logic error in condition

---

## Root Cause

`GossipingPropertyFileSnitch` periodically reloads its config file and compares the new DC/rack strings with the current values to decide whether gossip state needs to be re-announced. The comparison used `!=` (reference identity) instead of `!equals()`. Since the new strings are freshly read from the properties file, they are distinct `String` objects even when the content is identical, so the condition was always true and gossip was re-announced on every reload tick regardless of whether anything changed.

### Before / After

```java
// BEFORE
if (myDC != newDc || myRack != newRack || (preferLocal != newPreferLocal))
```

```java
// AFTER
if (!newDc.equals(myDC) || !newRack.equals(myRack) || (preferLocal != newPreferLocal))
```

---

## How to Spot

String comparisons with `==` or `!=` in Java almost always indicate a bug unless the strings are known to be interned constants. Code reviewers should flag any `==`/`!=` comparison involving `String` variables read from external sources (files, network, config).

---

## Test

No test added.
