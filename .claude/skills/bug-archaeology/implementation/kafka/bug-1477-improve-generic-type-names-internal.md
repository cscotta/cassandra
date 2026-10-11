**Commit**: 7719b5f70d · **Ticket**: KAFKA-18644
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Lucas Brutschy <lbrutschy@confluent.io>

**Before/After**
```java
// Before

public class CombinedKey<KF, KP> {
    private final KF foreignKey;
    private final KP primaryKey;
    CombinedKey(final KF foreignKey, final KP primaryKey) {
    public KF foreignKey() {
    public KP primaryKey() {
public class CombinedKeySchema<KO, K> {
    private Serializer<K> primaryKeySerializer;
    private Deserializer<K> primaryKeyDeserializer;
    private Serializer<KO> foreignKeySerializer;
    private Deserializer<KO> foreignKeyDeserializer;

// After
public class CombinedKey<KRight, KLeft> {
    private final KRight foreignKey;
    private final KLeft primaryKey;
    CombinedKey(final KRight foreignKey, final KLeft primaryKey) {
    public KRight foreignKey() {
    public KLeft primaryKey() {
public class CombinedKeySchema<KRight, KLeft> {
    private Serializer<KLeft> primaryKeySerializer;
    private Deserializer<KLeft> primaryKeyDeserializer;
    private Serializer<KRight> foreignKeySerializer;
    private Deserializer<KRight> foreignKeyDeserializer;
                             final Serde<KRight> foreignKeySerde,
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18644: improve generic type names for internal fk-join classes (#18700).

**Test**
Test modified in SubscriptionSendProcessorSupplierTest.java covering the corrected behaviour.
