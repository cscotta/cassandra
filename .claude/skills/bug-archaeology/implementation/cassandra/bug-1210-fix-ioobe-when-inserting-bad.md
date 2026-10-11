# Bug 1210: IndexOutOfBoundsException When Inserting Tuple with Too Many Elements via String Literal

**Commit**: c233270643 · **Ticket**: CASSANDRA-9559
**Subsystem**: src/java/org/apache/cassandra/db/marshal/TupleType.java
**Category**: Missing null / bounds check

---

## Root Cause

`TupleType.fromString()` splits the raw string literal on unescaped `:` separators to obtain per-field strings, then allocates a `ByteBuffer[]` sized to the number of parsed fields and iterates over them calling `types.get(i)` to find each field's deserializer. When the input contains more fields than the tuple type declares, `types.get(i)` throws an `IndexOutOfBoundsException` because there is no bounds check before the loop. The fix adds an explicit size comparison immediately after splitting, converting the crash into a descriptive `MarshalException`.

### Before / After

```java
// BEFORE
List<String> fieldStrings = AbstractCompositeType.split(source);
ByteBuffer[] fields = new ByteBuffer[fieldStrings.size()];
for (int i = 0; i < fieldStrings.size(); i++)
{
    // types.get(i) throws IOOBE when i >= size()
```

```java
// AFTER
List<String> fieldStrings = AbstractCompositeType.split(source);

if (fieldStrings.size() > size())
    throw new MarshalException(String.format(
        "Invalid tuple literal: too many elements. Type %s expects %d but got %d",
        asCQL3Type(), size(), fieldStrings.size()));

ByteBuffer[] fields = new ByteBuffer[fieldStrings.size()];
for (int i = 0; i < fieldStrings.size(); i++)
{
```

---

## How to Spot

Any loop that indexes into a fixed-size type-descriptor collection using a count derived from user input needs a pre-loop size assertion. The pattern `for (int i = 0; i < userSupplied.size(); i++) { typeList.get(i) ... }` is a classic off-by-one vulnerability when `userSupplied` can exceed `typeList`.

---

## Test

`TupleTypeTest.testTupleFromString` exercises round-trip string inserts for valid tuples and asserts that inserting a string literal with too many fields (e.g., `'1:2:3'` into a `tuple<int, text>`) throws an `InvalidRequestException` with the expected message. An existing test for the CQL literal form is also tightened to assert the exact message.
