**Commit**: edd3860433 · **Ticket**: KAFKA-17764
**Subsystem**: clients/share (ShareFetch API schema)
**Category**: Wrong serialization / deserialization

**Root cause**
`ShareFetchRequest.json` and `ShareFetchResponse.json` annotated several fields with `"ignorable": true`. This annotation means "a client that does not understand this field can safely ignore it", which is only meaningful when the field was absent in earlier protocol versions. Because the Share Fetch API has only one version, every field is present in every version; marking them ignorable was misleading metadata that could confuse code generators and protocol documentation tools.

**Before/After**
```json
// Before
{ "name": "MaxBytes", "type": "int32", "versions": "0+",
  "ignorable": true, "about": "..." }

// After
{ "name": "MaxBytes", "type": "int32", "versions": "0+",
  "about": "..." }
```

**How to spot**
When adding a single-version API, do not copy `"ignorable": true` from older multi-version schemas; ignorable is only correct when the field genuinely did not exist in a prior version that the protocol still supports.

**Test**
No test added.
