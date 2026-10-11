# Bug 0634: SSTableLoader Log Message Missing Space Between Words

**Commit**: f767d35ae9 · **Ticket**: CASSANDRA-14003
**Subsystem**: `io/sstable/SSTableLoader.java`
**Category**: Wrong constant / default value

---

## Root Cause

The format string passed to `outputHandler.output()` concatenated the `%s` placeholder for the SSTable names directly against the word `"to"` without a separating space, producing log output like `"Streaming relevant part of [table1.db]to [host1]"`. The omitted space was a copy-paste or typing error that made the log line hard to read and parse.

### Before / After

```java
// BEFORE
outputHandler.output(String.format(
    "Streaming relevant part of %sto %s",
    names(sstables), endpointToRanges.keySet()));
```

```java
// AFTER
outputHandler.output(String.format(
    "Streaming relevant part of %s to %s",
    names(sstables), endpointToRanges.keySet()));
```

---

## How to Spot

Format strings that embed `%s` immediately adjacent to a word — with no trailing space inside the string literal — will produce run-together output whenever the formatted value does not itself end in whitespace. Reviewers should read format strings aloud or scan for `%s<letter>` or `<letter>%s` patterns without surrounding spaces.

---

## Test

No test added.
