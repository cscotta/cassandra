# Bug 0671: Integer Overflow Reporting Filesystem Size on Exabyte-Scale Volumes

**Commit**: 270f690ff6 · **Ticket**: CASSANDRA-13067
**Subsystem**: src/java/org/apache/cassandra/io/util/FileUtils.java · config/DatabaseDescriptor.java · db/Directories.java
**Category**: Wrong constant / default value

---

## Root Cause

Java's `FileStore.getTotalSpace()`, `getUnallocatedSpace()`, and `File.getUsableSpace()` return `long` values. On filesystems larger than 2^63-1 bytes (≈9.2 EB) the JDK itself overflows and returns a negative number (JDK-8162520 / JDK-8179320). Cassandra used these values directly in arithmetic: summing free space across multiple data directories with `+=` could overflow to a negative total, and `Ints.checkedCast()` on the commitlog sizing calculation would throw `IllegalArgumentException` for a negative dividend. The fix introduces a `SafeFileStore` decorator that clamps values to `Long.MAX_VALUE` on overflow, a `saturatedSum` helper for accumulating per-directory free space, and replaces `Ints.checkedCast` with `Ints.saturatedCast` to gracefully handle the saturated long.

### Before / After

```java
// BEFORE — overflow possible; checkedCast throws on negative input
minSize = Ints.checkedCast(
    (guessFileStore(conf.commitlog_directory).getTotalSpace() / 1048576) / 4);

dataFreeBytes += guessFileStore(datadir).getUnallocatedSpace();  // sum can overflow
```

```java
// AFTER — saturating arithmetic throughout
minSize = Ints.saturatedCast(
    (guessFileStore(conf.commitlog_directory).getTotalSpace() / 1048576) / 4);

dataFreeBytes = saturatedSum(dataFreeBytes, guessFileStore(datadir).getUnallocatedSpace());

// FileUtils.getFileStore() returns a SafeFileStore that clamps to Long.MAX_VALUE
```

---

## How to Spot

Any code that reads filesystem size via Java's `FileStore` or `File` APIs and then uses the result in arithmetic without overflow guards is vulnerable on very large volumes. Look for `getTotalSpace()`, `getUnallocatedSpace()`, `getUsableSpace()`, and `getFreeSpace()` calls followed by unchecked addition, multiplication, or cast to `int`.

---

## Test

No test added (the bug is a platform-level overflow that is hard to reproduce without an exabyte filesystem).
