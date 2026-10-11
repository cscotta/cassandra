# Bug 1370: Index Name Case Sensitivity Lost on CREATE/DROP INDEX

**Commit**: 68be72fdc6 · **Ticket**: CASSANDRA-8365
**Subsystem**: src/java/org/apache/cassandra/cql3/IndexName.java
**Category**: Logic error in condition

---

## Root Cause

`CREATE INDEX` parsed the index name using the bare `IDENT` grammar rule, which always produced a lowercase token, bypassing the `QUOTED_NAME` path that preserves case. This meant `CREATE INDEX "CamelCase" ON ...` silently folded the name to `camelcase`, and a subsequent `DROP INDEX "CamelCase"` would fail to match. Both `CFName` and `IndexName` duplicated the same case-folding logic independently rather than delegating to a shared base class, making the fix non-obvious without inspecting the grammar. The root structural vulnerability was that index name handling was not factored through the same `keepCase` machinery as keyspace and table names.

### Before / After

```java
// BEFORE (Cql.g)
// createIndexStatement only accepted IDENT for the index name,
// stripping case regardless of quoting
(idxName=IDENT)? K_ON cf=columnFamilyName ...
{ $expr = new CreateIndexStatement(cf, $idxName.text, id, props, ifNotExists); }
```

```java
// AFTER (Cql.g)
// idxName rule handles IDENT, QUOTED_NAME, and unreserved keywords
// with proper keepCase routing—mirroring cfName and ksName rules
(idxName[name])? K_ON cf=columnFamilyName ...
{ $expr = new CreateIndexStatement(cf, name, id, props, ifNotExists); }
```

---

## How to Spot

Whenever a CQL grammar rule accepts a name token, check whether that rule reaches the `QUOTED_NAME` alternative in addition to bare `IDENT`. A name that only matches `IDENT` will always be lowercased; if the corresponding `DROP` or `ALTER` statement honours quoting, the two paths will diverge. The fix here—adding a shared `KeyspaceElementName` base class and a dedicated `idxName` grammar rule—is the canonical pattern to follow for any new schema-element names.

---

## Test

New `CreateIndexStatementTest` added covering plain identifiers, quoted identifiers, and camelCase identifiers for both `CREATE INDEX` and `DROP INDEX`, with and without explicit keyspace qualifier on the drop.
