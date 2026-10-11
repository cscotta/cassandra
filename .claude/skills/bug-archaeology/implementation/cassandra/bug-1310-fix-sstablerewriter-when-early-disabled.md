# Bug 1310: SSTableRewriter Broken When Early Re-open Is Disabled

**Commit**: d97e7cb69e · **Ticket**: CASSANDRA-8535
**Subsystem**: io/sstable/SSTableRewriter
**Category**: Logic error in condition

---

## Root Cause

`SSTableRewriter` has two modes: a normal mode where partially-written SSTables are periodically re-opened early (preemptive open), and a disabled mode where `preemptiveOpenInterval` is set to `Long.MAX_VALUE`. The `switchWriter` and `finish` code paths unconditionally assumed preemptive open was active: when a writer was finalized during `switchWriter`, it always called `writer.finish(FinishType.EARLY, ...)` and added the result to `finishedEarly`, then `finish()` iterated `finishedEarly` to promote those readers. When early re-open was disabled, `maybeReopenEarly` also checked `!FBUtilities.isWindows()` rather than testing `preemptiveOpenInterval`, so the guard was inconsistent. The result was that SSTable rewrites with early open disabled either lost the produced SSTable or threw errors during the finish phase. The fix adds a `finishedReaders` list for the non-early path, routes `switchWriter` through it when `preemptiveOpenInterval == Long.MAX_VALUE`, unifies the Windows check into the interval check, and adds an early-return path in `finish` that promotes directly from `finishedReaders`.

### Before / After

```java
// BEFORE – always uses EARLY finish regardless of interval
SSTableReader reader = writer.finish(SSTableWriter.FinishType.EARLY, maxAge, -1);
replaceEarlyOpenedFile(currentlyOpenedEarly, reader);
moveStarts(reader, reader.last, false);
finishedEarly.add(new Finished(writer, reader));
```

```java
// AFTER – branches on whether early re-open is active
if (preemptiveOpenInterval == Long.MAX_VALUE)
{
    SSTableReader reader = writer.finish(SSTableWriter.FinishType.NORMAL, maxAge, -1);
    finishedReaders.add(reader);
}
else
{
    SSTableReader reader = writer.finish(SSTableWriter.FinishType.EARLY, maxAge, -1);
    replaceEarlyOpenedFile(currentlyOpenedEarly, reader);
    moveStarts(reader, reader.last, false);
    finishedEarly.add(new Finished(writer, reader));
}
```

---

## How to Spot

When a class has a feature flag that can effectively disable a subsystem (here, `preemptiveOpenInterval == Long.MAX_VALUE`), every code path that touches the controlled subsystem must branch on that flag consistently. A reviewer should search for all sites that reference `finishedEarly` and `currentlyOpenedEarly` and confirm they each check the same condition—an inconsistent guard (like the `!isWindows()` test) is a red flag.

---

## Test

No test added.
