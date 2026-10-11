# Bug 1816: Fix Merge — Missing Import in SliceQueryPager

**Commit**: 1a5ebd6a7d · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/pager
**Category**: Logic error in condition

---

## Root Cause

A bad merge left `SliceQueryPager.java` missing the `import java.nio.ByteBuffer` statement that was required after the `key()` method returning `ByteBuffer` was added in the preceding commit (7c32ffbbfa). Without the import the class would not compile, making the `key()` method — which is part of the `SinglePartitionPager` interface — inaccessible.

### Before / After

```java
// BEFORE (missing import)
package org.apache.cassandra.service.pager;

import java.util.Collections;
import java.util.List;
```

```java
// AFTER
package org.apache.cassandra.service.pager;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
```

---

## How to Spot

After any merge that introduces a new return type into an interface, verify that every implementing file has the corresponding import. A compilation error is the immediate symptom, but the pattern to catch during review is: new method added to interface → check all implementations for required imports.

---

## Test

No test added. The fix is a compile-time correction; the functional test coverage belongs to the parent commit (CASSANDRA-6464).
