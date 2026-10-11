# Bug 1002: SSTableLoader Fails with Uppercase Keyspace Name

**Commit**: ed96322a0d · **Ticket**: CASSANDRA-10806
**Subsystem**: SSTable tools / bulk loader
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`NativeSSTableLoaderClient` called `metadata.getReplicas(keyspace, tokenRange)` using the raw keyspace string supplied on the command line.
The Java driver's `Metadata.getReplicas` requires the keyspace name to be quoted with `Metadata.quote()` when the name contains uppercase letters, otherwise the internal lookup is case-folded and returns an empty set.
With an empty endpoint set the loader treated every token range as having no replicas and skipped streaming, producing a silent no-op load.

### Before / After
```java
// BEFORE — raw keyspace name, fails for mixed-case names:
Set<Host> endpoints = metadata.getReplicas(keyspace, tokenRange);

// AFTER — properly quoted for driver metadata lookup:
Set<Host> endpoints = metadata.getReplicas(Metadata.quote(keyspace), tokenRange);
```

---

## How to Spot
Whenever user-supplied identifier strings are passed directly to driver or system metadata APIs, check whether those APIs document a quoting or case-normalisation requirement. The Java driver's `Metadata.getReplicas` and `Metadata.getKeyspace` calls both require `Metadata.quote()` for identifiers that are not already lower-case. The symptom — silent empty result rather than an exception — makes this class of bug easy to miss in functional testing unless an uppercase keyspace is explicitly exercised.

---

## Test
No test added.
