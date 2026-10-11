# Bug 0321: BulkLoadConnectionFactory Overrides Wrong Method Name After Merge

**Commit**: 754fc5d075 · **Ticket**: n/a (merge fix for CASSANDRA-17367 context)
**Subsystem**: tools/BulkLoadConnectionFactory
**Category**: Missing override of default method

---

## Root Cause

`BulkLoadConnectionFactory` extends `NettyStreamingConnectionFactory` and is supposed to override the `StreamingChannel.Factory#create(InetSocketAddress, int, Kind)` interface method. A bad branch merge left the overriding method named `connect` instead of `create`, meaning `BulkLoadConnectionFactory.connect` was a dead, never-called method while the superclass `create` implementation (without SSL routing or encryption settings) was silently invoked instead. No `@Override` annotation was present to catch the mismatch at compile time.

### Before / After

```java
// BEFORE
public NettyStreamingChannel connect(InetSocketAddress to, int messagingVersion,
                                     StreamingChannel.Kind kind) throws IOException
```

```java
// AFTER
public NettyStreamingChannel create(InetSocketAddress to, int messagingVersion,
                                    StreamingChannel.Kind kind) throws IOException
```

---

## How to Spot

Every method intended to override a superclass or interface method should carry `@Override`; the compiler then catches name mismatches immediately. During merges that rename interface methods, check all implementing classes for stale method names. A static analysis pass that flags non-`@Override` public methods whose signature matches a supertype method (modulo name) would surface this class of bug.

---

## Test

No new test added; the companion commit (CASSANDRA-17367) added an integration test that streams real data over SSL, which fails when the wrong method is called.
