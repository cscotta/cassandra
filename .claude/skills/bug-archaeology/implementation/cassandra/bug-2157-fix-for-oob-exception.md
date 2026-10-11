# Bug 2157: IndexHelper.indexFor Returns Out-of-Bounds Index Causing ArrayIndexOutOfBoundsException

**Commit**: f91001a085 · **Ticket**: CASSANDRA-5030
**Subsystem**: src/java/org/apache/cassandra/io/sstable/IndexHelper.java, src/java/org/apache/cassandra/db/columniterator/SSTableNamesIterator.java
**Category**: Missing null / bounds check

---

## Root Cause

`IndexHelper.indexFor` uses a `lastIndex` hint to skip ahead in a binary search over index blocks. When `lastIndex` was already equal to or beyond `indexList.size()`, the binary search starting position was out of range, causing the method to return an invalid index that later triggered an `ArrayIndexOutOfBoundsException` in `SSTableNamesIterator`. Two guards were missing: `indexFor` did not check whether `lastIndex >= indexList.size()` before proceeding, and the caller did not check for a negative return value (which the method could already return for the reversed case but was not handled for the forward case).

### Before / After

```java
// BEFORE — IndexHelper.indexFor: no bounds check on lastIndex
if (name.remaining() == 0 && reversed)
    return indexList.size() - 1;
IndexInfo target = new IndexInfo(name, name, 0, 0);
// ... proceeds even if lastIndex >= indexList.size()
```

```java
// AFTER
if (name.remaining() == 0 && reversed)
    return indexList.size() - 1;

if (lastIndex >= indexList.size())
    return -1;
IndexInfo target = new IndexInfo(name, name, 0, 0);
```

```java
// BEFORE — SSTableNamesIterator: only checked upper bound
if (index == indexList.size())
    continue;
```

```java
// AFTER — also checks for the -1 sentinel
if (index < 0 || index == indexList.size())
    continue;
```

---

## How to Spot

Whenever a search method accepts a "hint" or "start" index parameter, verify the method guards against the hint being out of range before using it. Callers receiving a sentinel return value (e.g., `-1`) must check for it before using the value as an array index.

---

## Test

`IndexHelperTest` was extended to cover the case where `lastIndex` equals `indexList.size()`, asserting that `-1` is returned instead of an out-of-bounds value.
