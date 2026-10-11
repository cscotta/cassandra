# Bug 1977: ConcurrentModificationException during streaming due to iterating live collection

**Commit**: c7220e3a7b · **Ticket**: CASSANDRA-5782
**Subsystem**: streaming/StreamSession.java, streaming/StreamTransferTask.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`StreamTransferTask.getFileMessages()` returned `files.values()`, a live view of the underlying `ConcurrentHashMap`. When `StreamSession` iterated this collection to enqueue file messages for sending, concurrent completion callbacks could remove entries from `files` mid-iteration, throwing `ConcurrentModificationException`. The fix copies the values into a new `ArrayList` before returning so the caller iterates a stable snapshot.

### Before / After

```java
// BEFORE: returns live map view — caller iterates while completions remove entries
public Collection<FileMessage> getFileMessages() {
    return files.values();
}
```

```java
// AFTER: snapshot copied before returning
public Collection<FileMessage> getFileMessages() {
    // We may race between queuing all those messages and the completion of
    // the first ones. So copy the values to avoid a ConcurrentModificationException
    return new ArrayList<>(files.values());
}
```

---

## How to Spot

Any time a collection's `.values()` / `.keySet()` / `.entrySet()` is returned from a method and the underlying map may be modified by another thread while the caller iterates, the caller needs a defensive copy at the point of access.

---

## Test

No test added.
