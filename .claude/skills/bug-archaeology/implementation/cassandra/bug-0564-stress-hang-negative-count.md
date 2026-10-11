# Bug 0564: cassandra-stress Hangs When Count Is Negative (Default/Unlimited)

**Commit**: bbf7dac87c · **Ticket**: CASSANDRA-14616
**Subsystem**: tools/stress/src/org/apache/cassandra/stress
**Category**: Logic error in condition

---

## Root Cause

`StressAction.warmup` computed the number of warmup iterations as `Math.min(50000, (int)(settings.command.count * 0.25)) * settings.node.nodes.size()`. When the user runs with default options, `settings.command.count` is `-1` (meaning "run forever"), so the multiplication yields a large negative integer, `Math.min` returns that negative value, and the product with `nodes.size()` is still negative. The subsequent code spun waiting for a `CountDownLatch` whose count was never reached because the "warmup" was launched with a negative or zero iteration count, causing the tool to hang indefinitely.

### Before / After

```java
// BEFORE
int iterations = Math.min(50000, (int)(settings.command.count * 0.25))
                 * settings.node.nodes.size();
```

```java
// AFTER
int iterations = (settings.command.count >= 0
                  ? Math.min(50000, (int)(settings.command.count * 0.25))
                  : 50000) * settings.node.nodes.size();
if (iterations <= 0) return;
```

---

## How to Spot

Any use of a sentinel negative value (such as `-1` for "unlimited") inside arithmetic expressions that feed into loop counts or latch countdown values is dangerous. Reviewers should look for places where a field that is documented to be `-1` when unbounded is multiplied or compared without a prior sign check. An early guard — `if (iterations <= 0) return;` — is a standard defensive pattern that makes such code robust against future changes to the sentinel value.

---

## Test

No test added.
