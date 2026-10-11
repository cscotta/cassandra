# Bug 0034: Table Names Not Validated Against Filesystem Length Limit

**Commit**: 6c29686ea7 · **Ticket**: CASSANDRA-20389
**Subsystem**: schema, db
**Category**: Missing null / bounds check

---

## Root Cause

Cassandra stored table data in directories whose names are derived from `<table_name>-<table_uuid_hex>`. The UUID hex string occupies 36 characters and the separator one more, consuming 37 characters of the 255-character filesystem filename limit, leaving 218 characters for the table name. The existing validation functions only checked a generic 48-character limit applicable to keyspace/table identifiers as CQL names, and these validation paths were confused: a method named `isValidName` checked characters but not length, while a separate length-checking path was never called during table creation. As a result, a table name longer than 218 characters would pass schema validation but cause an OS-level "file name too long" error when Cassandra tried to create the data directory.

### Before / After

```java
// BEFORE: TableMetadata.validate() — no table-name-specific length check
// isValidName() checked format, not the filesystem-derived limit
```

```java
// AFTER: new constant and dedicated check
public static final int TABLE_NAME_LENGTH =
    FILENAME_LENGTH - TABLE_UUID_AS_HEX_LENGTH - TABLE_DIRECTORY_NAME_SEPARATOR.length();

// TableMetadata.validate() now calls:
validateTableNameLength(name);
```

---

## How to Spot

When a schema element is used to construct a filesystem path, the length validation must account for all fixed-length suffixes appended to the user-supplied name (UUIDs, separators, extensions). A general "name must be ≤ 48 chars" check is insufficient if the file path construction imposes a stricter constraint — each distinct usage of a name that feeds into a filesystem path needs its own upper-bound constant derived from that path's composition.

---

## Test

`CreateTableValidationTest` adds tests that attempt to create tables with names at and above the `TABLE_NAME_LENGTH` boundary and assert the correct `InvalidRequestException` is thrown.
