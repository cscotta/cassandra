**Commit**: dbd50ff847 · **Ticket**: KAFKA-16469
**Subsystem**: checkstyle/import-control.xml
**Category**: Wrong serialization / deserialization

**Root cause**
Create a schema checker that can validate that later versions of a KRPC schema are compatible with earlier ones. Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
              files="(SchemaGenerator|MessageDataGenerator|FieldSpec|FieldType).java"/>
              files="(MessageDataGenerator|FieldSpec|WorkerSinkTask).java"/>
    static final ObjectMapper JSON_SERDE;
    static String capitalizeFirst(String string) {
final class StructRegistry {
    StructRegistry() {
    void register(MessageSpec message) {
    StructSpec findStruct(FieldSpec field) {

// After
    <allow pkg="org.apache.kafka.message" />
              files="(SchemaGenerator|MessageDataGenerator|FieldSpec|FieldSpecPairIterator|FieldType).java"/>
              files="(FieldSpecPairIterator|MessageDataGenerator|FieldSpec|WorkerSinkTask).java"/>
            // Check struct invariants
            if (this.type.isStruct() || this.type.isStructArray()) {
                new StructSpec(name,
                    versions,
                    Versions.NONE_STRING, // version deprecations not supported at field level
                    fields);
            }
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
    public static final ObjectMapper JSON_SERDE;
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16469: metadata schema checker (#15995).

**Test**
Test modified in StructSpecTest.java covering the corrected behaviour.
