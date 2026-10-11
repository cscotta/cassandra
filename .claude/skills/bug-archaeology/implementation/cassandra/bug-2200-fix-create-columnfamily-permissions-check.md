# Bug 2200: CREATE COLUMNFAMILY Permission Check Uses Column Family Name Instead of Keyspace

**Commit**: a2a2251b37 · **Ticket**: CASSANDRA-4864
**Subsystem**: src/java/org/apache/cassandra/cql/QueryProcessor.java
**Category**: Wrong constant / default value

---

## Root Cause

When handling a `CREATE COLUMNFAMILY` CQL statement, the permissions check passed the column family name (`createCf.getName()`) to `hasColumnFamilySchemaAccess` instead of the keyspace name. Permission checks for schema operations require the keyspace as the resource identifier, so passing the table name caused the authorization to evaluate against the wrong resource, potentially allowing or denying the operation incorrectly.

### Before / After

```java
// BEFORE
clientState.hasColumnFamilySchemaAccess(createCf.getName(), Permission.CREATE);
```

```java
// AFTER
clientState.hasColumnFamilySchemaAccess(keyspace, Permission.CREATE);
```

---

## How to Spot

When a method call takes a name-like argument, verify that the argument represents the correct scope (keyspace vs. column family name). Schema permission checks are typically keyed on the keyspace, not the object being created within it.

---

## Test

No test added.
