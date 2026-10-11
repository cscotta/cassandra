# Bug 0889: Duplicate Channel Attribute Lookup in Frame Decoder

**Commit**: 4ae198765b · **Ticket**: CASSANDRA-11969
**Subsystem**: Native Transport / Netty
**Category**: Logic error in condition

---

## Root Cause
In `Frame.Decoder.decode()`, the first frame on a new channel triggers a branch that calls `ctx.channel().attr(Connection.attributeKey)` twice — once to read the existing value (which returned null) and a second time to write the newly constructed `Connection` object. Each call to `ctx.channel().attr()` is not guaranteed to be free of side effects (it allocates or looks up a map entry inside Netty's `DefaultAttributeMap`), so the two independent calls represent redundant work and, more importantly, introduce a latent hazard if the attribute map ever moves to a lock-free scheme where two separate get/set calls on the same key are not atomic. The fix caches the `Attribute<Connection>` reference in a local variable and reuses it for both the null-check and the set.

### Before / After
```java
// BEFORE — two separate ctx.channel().attr() calls
Connection connection = ctx.channel().attr(Connection.attributeKey).get();
if (connection == null) {
    connection = factory.newConnection(ctx.channel(), version);
    ctx.channel().attr(Connection.attributeKey).set(connection);
}

// AFTER — single attribute lookup, result reused
Attribute<Connection> attrConn = ctx.channel().attr(Connection.attributeKey);
Connection connection = attrConn.get();
if (connection == null) {
    connection = factory.newConnection(ctx.channel(), version);
    attrConn.set(connection);
}
```

---

## How to Spot
When reviewing Netty pipeline handlers, any two consecutive calls to `ctx.channel().attr(sameKey)` in the same method should be collapsed into a single `Attribute<T>` reference. The pattern is especially suspicious when the first call is used for a null-check and the second for an unconditional set, since it suggests the reviewer mentally treated `attr()` as a field access when it is actually a method call.

---

## Test
No test added.
