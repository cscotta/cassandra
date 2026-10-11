# Bug 1533: Fix Column Name Serialization for cassandra-stress/stressd

**Commit**: a182ea00ba · **Ticket**: CASSANDRA-7608
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/settings/SettingsColumn.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

`SettingsColumn.names` is a `List<ByteBuffer>` marked `transient`, meaning Java's default serialization skipped it entirely. When `SettingsColumn` was deserialized on the stressd daemon the `names` field was `null`, causing NullPointerExceptions when the tool tried to build thrift-style table schemas. The fix adds custom `writeObject`/`readObject` methods that serialize `names` as a `List<byte[]>` (a serializable form) and reconstruct the `ByteBuffer` list on read. The field is also changed from `final` to non-final so `readObject` can assign it.

### Before / After

```java
// BEFORE
public transient final List<ByteBuffer> names;
// No writeObject/readObject — names silently lost across serialization
```

```java
// AFTER
public transient List<ByteBuffer> names;

private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.defaultWriteObject();
    ArrayList<byte[]> namesBytes = new ArrayList<>();
    for (ByteBuffer buffer : this.names)
        namesBytes.add(ByteBufferUtil.getArray(buffer));
    oos.writeObject(namesBytes);
}

private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    ois.defaultReadObject();
    List<byte[]> namesBytes = (List<byte[]>) ois.readObject();
    List<ByteBuffer> namesBuffer = new ArrayList<>();
    for (byte[] bytes : namesBytes)
        namesBuffer.add(ByteBuffer.wrap(bytes));
    this.names = new ArrayList<>(namesBuffer);
}
```

---

## How to Spot

In any `Serializable` class, a `transient` field that is set only in the constructor and never re-derived after deserialization will be silently `null` on the receiving end. Review every `transient` field and ask whether it needs custom `readObject` logic or can be recomputed from other non-transient fields.

---

## Test

No test added.
