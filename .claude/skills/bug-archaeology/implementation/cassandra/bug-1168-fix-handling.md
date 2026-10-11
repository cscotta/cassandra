# Bug 1168: Enable/Disable Autocompaction Not Propagated to Inner Strategies

**Commit**: dce303bcb5 · **Ticket**: CASSANDRA-9899
**Subsystem**: src/java/org/apache/cassandra/db/compaction/WrappingCompactionStrategy.java
**Category**: Missing override of default method

---

## Root Cause

`WrappingCompactionStrategy` wraps two inner strategy instances (`repaired` and `unrepaired`) but did not override `enable()` and `disable()`. When those methods were called on the outer wrapper, only the wrapper's own flag changed; the inner strategies kept their previous enabled state and continued scheduling (or refusing to schedule) compactions independently. Additionally, when the strategy was reloaded via `reloadCompactionStrategy()`, any JMX-driven disabled state was silently discarded because the method never consulted the wrapper's enabled flag before recreating and starting the inner strategies. Each concrete strategy also contained its own redundant `isEnabled()` guard inside `getNextBackgroundTask`, meaning the enable/disable decision was scattered across every subclass rather than being enforced in one place.

### Before / After

```java
// BEFORE: reloadCompactionStrategy did not preserve enabled state,
// and each subclass guarded independently
public synchronized AbstractCompactionTask getNextBackgroundTask(int gcBefore) {
    if (!isEnabled())
        return null;
    // ...
}
```

```java
// AFTER: wrapper owns enable/disable and propagates to inner strategies;
// subclass guards removed
@Override
public void enable() {
    if (repaired != null) repaired.enable();
    if (unrepaired != null) unrepaired.enable();
    super.enable(); // enable wrapper last
}

@Override
public void disable() {
    super.disable(); // disable wrapper first
    if (repaired != null) repaired.disable();
    if (unrepaired != null) unrepaired.disable();
}

public synchronized void reloadCompactionStrategy(CFMetaData metadata) {
    boolean disabledWithJMX = !isEnabled() && shouldBeEnabled();
    // ... recreate inner strategies ...
    if (disabledWithJMX || !shouldBeEnabled())
        disable();
    else
        enable();
}
```

---

## How to Spot

When a class wraps a set of delegate objects and inherits default behaviour from a base class, any method that modifies state on `this` without forwarding to the delegates is a candidate for a missing propagation bug. Look for `enable`/`disable`, `start`/`stop`, or similar lifecycle pairs: if the wrapper class does not explicitly override them, state changes affect only the wrapper's flag and are invisible to the inner objects. A code search for `super.enable()` or `super.disable()` absent in wrapper subclasses is a reliable heuristic.

---

## Test

No dedicated test added in this commit.
