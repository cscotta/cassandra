# Bug 0033: Wrong Constant in Keyspace Name Length Error Message

**Commit**: 90f231ad56 · **Ticket**: CASSANDRA-20915
**Subsystem**: schema
**Category**: Wrong constant / default value

---

## Root Cause

`KeyspaceMetadata.validateKeyspaceName` enforced the correct limit (`SchemaConstants.NAME_LENGTH = 48`) in the actual check, but the error message reported `TABLE_NAME_LENGTH` (a much larger filesystem-derived constant) instead of `NAME_LENGTH`. This meant users received a misleading error that quoted the wrong maximum when their keyspace name was too long.

### Before / After

```java
// BEFORE
if (keyspaceName.length() > SchemaConstants.NAME_LENGTH)
    throw exceptionBuilder.apply(format(
        "Keyspace name must not be more than %d characters long (got %d characters for \"%s\")",
        TABLE_NAME_LENGTH, keyspaceName.length(), keyspaceName));
```

```java
// AFTER
if (keyspaceName.length() > SchemaConstants.NAME_LENGTH)
    throw exceptionBuilder.apply(format(
        "Keyspace name must not be more than %d characters long (got %d characters for \"%s\")",
        SchemaConstants.NAME_LENGTH, keyspaceName.length(), keyspaceName));
```

---

## How to Spot

When an error message quotes a threshold that was just used in the preceding conditional, verify that the constant in the message matches the constant in the condition — they are separate expressions and IDE auto-complete or copy-paste can substitute a different but similarly-named constant without a compile error.

---

## Test

`KeyspaceMetadataTest.testValidateKeyspaceNameTooLongShowsCorrectLimit` asserts the exception message contains `NAME_LENGTH` (48), not the larger `TABLE_NAME_LENGTH`.
