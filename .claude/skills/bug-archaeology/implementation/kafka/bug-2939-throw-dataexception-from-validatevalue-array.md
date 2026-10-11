**Commit**: 52514a89e3 · **Ticket**: KAFKA-16858
**Subsystem**: connect/api
**Category**: Wrong serialization / deserialization

**Root cause**
Signed-off-by: Greg Harris <greg.harris@aiven.io>
Reviewers: Chris Egerton <chrise@aiven.io>

**Before/After**
```java
// Before
    public static void validateValue(String name, Schema schema, Object value) {
                throw new DataException("Invalid value: null used for required field: \"" + name
                        + "\", schema type: " + schema.type());
            if (name != null) {
                exceptionMessage.append(" for field: \"").append(name).append("\"");
                for (Object entry : array)
                    validateValue(schema.valueSchema(), entry);
                    validateValue(schema.keySchema(), entry.getKey());
                    validateValue(schema.valueSchema(), entry.getValue());

// After
    public static void validateValue(String field, Schema schema, Object value) {
        validateValue(schema, value, field == null ? "value" : "field: \"" + field + "\"");
    }

    private static void validateValue(Schema schema, Object value, String location) {
                throw new DataException("Invalid value: null used for required " + location
                        + ", schema type: " + schema.type());
            if (location != null) {
                exceptionMessage.append(" for ").append(location);
                String entryLocation = "element of array " + location;
                Schema arrayValueSchema = assertSchemaNotNull(schema.valueSchema(), entryLocation);
                for (Object entry : array) {
```

**How to spot**
When reviewing connect/api code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16858: throw dataexception from validatevalue on array and map schemas without inner schemas (#16161).

**Test**
Test modified in ConnectSchemaTest.java covering the corrected behaviour.
