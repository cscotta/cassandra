# Bug 1463: Possible Infinite Loop in createRepairRangeFrom

**Commit**: aa7794c83f · **Ticket**: CASSANDRA-7983
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: Logic error in condition

---

## Root Cause

`StorageService.createRepairRangeFrom` built a list of token ranges covering a user-specified `[beginToken, endToken)` interval by walking backwards from the predecessor of the end token. The while-loop condition `parsedBeginToken.compareTo(previous) < 0` terminates correctly for a normal ascending range, but when `beginToken > endToken` (a wrapping range that crosses the ring boundary) the predecessor walk never passes through `beginToken` and the loop runs indefinitely. The fix replaces the backward walk with a forward index scan: both tokens are inserted into a sorted copy of the ring token list, and the list is traversed from the index of `beginToken` to the index of `endToken` using modular arithmetic, which correctly handles wrapping ranges.

### Before / After

```java
// BEFORE
Token previous = tokenMetadata.getPredecessor(
    TokenMetadata.firstToken(tokenMetadata.sortedTokens(), parsedEndToken));
while (parsedBeginToken.compareTo(previous) < 0)
{
    repairingRange.addFirst(new Range<>(previous, parsedEndToken));
    parsedEndToken = previous;
    previous = tokenMetadata.getPredecessor(previous);
}
repairingRange.addFirst(new Range<>(parsedBeginToken, parsedEndToken));
```

```java
// AFTER
ArrayList<Token> tokens = new ArrayList<>(tokenMetadata.sortedTokens());
if (!tokens.contains(parsedBeginToken)) tokens.add(parsedBeginToken);
if (!tokens.contains(parsedEndToken))   tokens.add(parsedEndToken);
Collections.sort(tokens);
int start = tokens.indexOf(parsedBeginToken), end = tokens.indexOf(parsedEndToken);
for (int i = start; i != end; i = (i + 1) % tokens.size())
{
    repairingRange.add(new Range<>(tokens.get(i), tokens.get((i + 1) % tokens.size())));
}
```

---

## How to Spot

A backward predecessor walk that terminates on a less-than comparison will not terminate when the target token is numerically greater than the start token of the walk (wrapping case). Whenever a range-building algorithm accepts arbitrary begin/end tokens from user input it must handle the wrap-around case explicitly; a forward modular index scan over a fully sorted token list is the safe idiom.

---

## Test

`StorageServiceServerTest.testCreateRepairRangeFrom` was added. It covers a normal multi-segment range, a single-segment range entirely between two ring tokens, a two-segment range, a five-segment wrapping range, a range whose endpoints coincide with ring tokens, and a zero-length range (begin == end).
