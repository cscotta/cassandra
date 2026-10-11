# Bug 1081: Missing Semicolon Causes Compilation Failure in CqlInputFormat

**Commit**: af0678dd48 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3/CqlInputFormat.java
**Category**: Logic error in condition

---

## Root Cause

A wildcard import statement `import java.util.concurrent.*` was missing its terminating semicolon, causing a compile error in `CqlInputFormat`. The fix also reorganised the import block—splitting the broad `com.datastax.driver.core.*` wildcard import into explicit per-class imports and reordering the `org.apache.hadoop` imports—to eliminate the ambiguity and duplication that had accumulated across earlier merges and led to the broken statement.

### Before / After

```java
// BEFORE — missing semicolon, duplicate Row import
import java.util.concurrent.*
...
import com.datastax.driver.core.*;
...
import com.datastax.driver.core.Row;
```

```java
// AFTER — explicit, deduplicated imports with proper semicolons
import java.util.concurrent.*;

import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TokenRange;
```

---

## How to Spot

Wildcard imports (`.*`) that are missing their closing semicolon are a rare but legitimate Java syntax error that the compiler reports immediately. The risk is higher when auto-generated or auto-sorted import blocks are merged across branches, because a rebase or cherry-pick can silently drop or corrupt a single character. Any `Fix build` commit touching only import declarations is a signal to check for this class of truncation.

---

## Test

No test added.
