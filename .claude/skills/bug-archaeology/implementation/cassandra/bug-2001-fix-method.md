# Bug 2001: PermissionDetails.equals() Compared Object Identity Instead of Field Values

**Commit**: 08878e90ab · **Ticket**: CASSANDRA-5655
**Subsystem**: src/java/org/apache/cassandra/auth/PermissionDetails.java
**Category**: Missing override of default method

---

## Root Cause

`PermissionDetails.equals()` called `Objects.equal(this, o)`, which resolves to reference equality (`this == o`) because `Objects.equal` delegates to the objects' own `equals` — creating an infinite loop conceptually, or in practice falling back to identity comparison. Since `PermissionDetails` never actually compared its fields (`username`, `resource`, `permission`), two distinct instances with the same field values would never be considered equal, breaking any collection or deduplication logic that relied on this method.

### Before / After

```java
// BEFORE
@Override
public boolean equals(Object o) {
    return Objects.equal(this, o);
}
```

```java
// AFTER
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PermissionDetails)) return false;
    PermissionDetails pd = (PermissionDetails) o;
    return Objects.equal(this.username, pd.username)
        && Objects.equal(this.resource, pd.resource)
        && Objects.equal(this.permission, pd.permission);
}
```

---

## How to Spot

Any `equals()` override that calls `Objects.equal(this, x)` is self-referential and effectively useless — `Objects.equal` will call `this.equals(x)` again (or fall back to `==`). Review all `equals` overrides to ensure they compare fields of the two objects rather than delegating back to the object itself.

---

## Test

No test added.
