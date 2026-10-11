# Bug 2862: MurmurHash uses wrong array indices when offset is non-zero

**Commit**: 98d2f208d7 · **Ticket**: n/a

## Detection Rule
When implementing a hash loop over a byte array with a non-zero `offset`, keep the loop variable `i` zero-based (0 to `len_4`) and add `offset` only when indexing into `data[]`; iterating `i` from `offset` to `offset+len_4` while also using `i_4 = i<<2` double-counts the offset.

## Code Smell
```java
for (int i = offset; i < offset + len_4; i++) {
    int i_4 = i << 2;
    int k = data[i_4 + 3];  // i_4 already includes offset, so this is offset*4+3 off
```
Starting `i` at `offset` and computing `i_4 = i << 2` shifts the entire offset into byte positions. Combined with the constant offsets (+0/+1/+2/+3), this reads far outside the intended block.

## Tags
- scope: java
- category: off-by-one
- code-pattern: double-counted-array-offset
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
The MurmurHash implementation iterated from `i = offset` to `i < offset + len_4` and derived `i_4 = i << 2`. Since `i` already encodes the offset, `data[i_4 + N]` reads at position `(offset + block) * 4 + N` instead of `offset*4 + block*4 + N`. The fix resets `i` to zero-based and adds `offset` only in the array index: `data[offset + i_4 + N]`.

## Example
```java
// BEFORE
for (int i = offset; i < offset + len_4; i++) {
    int i_4 = i << 2;
    int k = data[i_4 + 3];
    k = k | (data[i_4 + 2] & 0xff);

// AFTER
for (int i = 0; i < len_4; i++) {
    int i_4 = i << 2;
    int k = data[offset + i_4 + 3];
    k = k | (data[offset + i_4 + 2] & 0xff);
```
