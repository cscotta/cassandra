# Bug 0591: SASI DelimiterAnalyzer Throws IllegalArgumentException on Empty Token

**Commit**: 7068ef6254 · **Ticket**: CASSANDRA-14450
**Subsystem**: `src/java/org/apache/cassandra/index/sasi/analyzer/DelimiterAnalyzer.java`
**Category**: Missing null / bounds check

---

## Root Cause

The `DelimiterAnalyzer` iterator read ahead to find the next delimiter and then constructed a `char[]` whose length was the span between the current position and the delimiter. When two delimiters were adjacent (empty token), the computed length was zero and `charset.encode(CharBuffer.wrap(new char[0]))` produced an empty `ByteBuffer`. That empty buffer was then passed to `ConcurrentRadixTree.putIfAbsent`, which throws `IllegalArgumentException` because blank partition keys are not allowed. The code also had a subtle bug: it checked `readahead.hasRemaining()` after the loop ended, but at that point the buffer position had already advanced, so the end-of-buffer sentinel was unreliable. The fix captures the `hasRemaining` result inside the loop condition and, when `chars.length == 0`, recurses via `computeNext()` to silently skip the empty token.

### Before / After

```java
// BEFORE — empty token causes IAE in radix tree insertion
while (readahead.hasRemaining() && readahead.get() != delimiter);
char[] chars = new char[readahead.position() - cb.position()
                         - (readahead.hasRemaining() ? 1 : 0)];
cb.get(chars);
return charset.encode(CharBuffer.wrap(chars));  // empty buffer if adjacent delimiters
```

```java
// AFTER — capture hasRemaining inside loop; skip empty tokens
boolean readaheadRemaining;
while ((readaheadRemaining = readahead.hasRemaining()) && readahead.get() != delimiter);
char[] chars = new char[readahead.position() - cb.position()
                         - (readaheadRemaining ? 1 : 0)];
cb.get(chars);
return 0 < chars.length
        ? charset.encode(CharBuffer.wrap(chars))
        : computeNext();   // skip blank partition key
```

---

## How to Spot

Any `AbstractIterator.computeNext()` that calls an external data structure with the result of a length-computed buffer should guard against zero-length inputs. When the iterator tokenises by delimiter, consecutive or leading/trailing delimiters always produce empty tokens and must be explicitly handled before the result is returned.

---

## Test

A new `testBlankEntries` test exercises a string with leading, trailing, and consecutive commas and asserts that only the non-empty tokens (`"Nip,it,in,the,bud"`) are returned.
