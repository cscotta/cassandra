# Bug 2064: Leveled Manifest Migration Fails to Strip Temporary Suffix from CF Name

**Commit**: d4744e178d · **Ticket**: CASSANDRA-5242
**Subsystem**: src/java/org/apache/cassandra/db/Directories.java
**Category**: Logic error in condition

---

## Root Cause

`Directories.migrateFile()` needed to extract the column family name from a leveled manifest filename in order to route the file to the right data directory. The original code stripped only the `.json` extension, but leveled manifest files can also carry a `-old` or `-tmp` suffix (e.g. `Users-old.json`, `Users-tmp.json`). When such a file existed, the extracted "cf name" included the suffix, causing a mismatch against the known column family name and leaving the file un-migrated.

### Before / After

```java
// BEFORE – suffix not removed
String cfname = isManifest
    ? name.substring(0, name.length() - LeveledManifest.EXTENSION.length())
    : name.substring(0, name.indexOf(Component.separator));
```

```java
// AFTER – helper strips -old / -tmp before returning
String cfname = isManifest
    ? getCfNameFromManifest(name)
    : name.substring(0, name.indexOf(Component.separator));

private static String getCfNameFromManifest(String name)
{
    String withoutExt = name.substring(0, name.length() - LeveledManifest.EXTENSION.length());
    return withoutExt.endsWith("-old") || withoutExt.endsWith("-tmp")
            ? withoutExt.substring(0, withoutExt.length() - 4)
            : withoutExt;
}
```

---

## How to Spot

When extracting a logical name by stripping a file extension, check whether the remaining string can still carry additional well-known suffixes (e.g. `-old`, `-tmp`, `.bak`). If the extracted name is used as a lookup key against a registry of known names, any unstripped suffix will silently produce a miss.

---

## Test

No test added.
