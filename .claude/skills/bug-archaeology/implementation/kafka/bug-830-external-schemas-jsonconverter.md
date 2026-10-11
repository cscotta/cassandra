**Commit**: eb9f5189f5 · **Ticket**: KAFKA-16913
**Subsystem**: connect/json
**Category**: Wrong serialization / deserialization

**Root cause**
When using a connector that requires a schema, such as JDBC connectors,
with JSON messages, the current JSONConverter necessitates including the
schema within every message. To address this, we are introducing a new
parameter, schema.content, which allows you to provide the schema
externally. This approach not only reduces the size of the messages but
also facilitates the use of more complex schemas. KIP :
https://cwiki.apache.org/confluence/display/KAFKA/KIP-1054%3A+Support+external+schemas+in+JSONConverter

Reviewers: Mickael Maison <mickael.maison@gmail.com>, TengYao Chi <frankvicky@apache.org>, Edoardo Comar <ECOMAR@uk.ibm.com>

**Before/After**
```java
// Before
        if (config.schemasEnabled() && (!jsonValue.isObject() || jsonValue.size() != 2 || !jsonValue.has(JsonSchema.ENVELOPE_SCHEMA_FIELD_NAME) || !jsonValue.has(JsonSchema.ENVELOPE_PAYLOAD_FIELD_NAME)))
            throw new DataException("JsonConverter with schemas.enable requires \"schema\" and \"payload\" fields and may not contain additional fields." +

        // The deserialized data should either be an envelope object containing the schema and the payload or the schema
        // was stripped during serialization and we need to fill in an all-encompassing schema.
        if (!config.schemasEnabled()) {

// After
    private Schema schema = null;     // if a schema is provided in config, this schema will be used for all messages

        try {
            final byte[] schemaContent = config.schemaContent();
            if (schemaContent != null) {
                final JsonNode schemaNode = deserializer.deserialize("", schemaContent);
                this.schema = asConnectSchema(schemaNode);
            }
        } catch (SerializationException e) {
            throw new DataException("Failed to parse schema in converter config due to serialization error: ", e);
        }
        if (config.schemasEnabled()) {
```

**How to spot**
When reviewing connect/json code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16913: support external schemas in jsonconverter (#19449).

**Test**
Test modified in JsonConverterTest.java covering the corrected behaviour.
