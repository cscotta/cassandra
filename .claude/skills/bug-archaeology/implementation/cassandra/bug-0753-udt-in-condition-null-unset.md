# Bug 0753: Null/unset IN-list marker crashes UDT condition binding

**Commit**: 3cf415279c · **Ticket**: n/a (follow-up to CASSANDRA-12981)
**Subsystem**: CQL / LWT conditions
**Category**: Missing null / bounds check

---

## Root Cause
In `ColumnCondition.CollectionBound` (the UDT/collection variant of IN-condition binding), `inValuesMarker.bind(options)` was cast directly to `Lists.Value` without first checking whether the bound result was `null` (representing a literal `null` list) or `Constants.UNSET_VALUE` (an unset marker). Both special sentinels cause a `ClassCastException` at the unchecked cast. The fix checks for these two cases immediately after the `bind()` call and throws `InvalidRequestException` with a descriptive message, matching the behaviour already implemented for the simpler scalar IN-condition path in the same commit's parent (CASSANDRA-12981). Additionally, the test helper calls for user-defined types were corrected from positional (`userType(0, "abc")`) to named-field syntax (`userType("a", 0, "b", "abc")`), which had been producing wrong comparison values.

### Before / After
```java
// BEFORE — unchecked cast; NPE or CCE on null/unset binding
for (ByteBuffer buffer : ((Lists.Value)inValuesMarker.bind(options)).elements)
    this.inValues.add(buffer);

// AFTER — null/unset guards before cast
Terminal terminal = inValuesMarker.bind(options);
if (terminal == null)
    throw new InvalidRequestException("Invalid null list in IN condition");
if (terminal == Constants.UNSET_VALUE)
    throw new InvalidRequestException("Invalid 'unset' value in condition");
for (ByteBuffer buffer : ((Lists.Value)terminal).elements)
    this.inValues.add(buffer);
```

---

## How to Spot
Wherever `Term.bind()` or `Marker.bind()` is used, always guard the result against `null` and `Constants.UNSET_VALUE` before casting or calling methods on it. Code that was written before the `UNSET` concept was introduced (protocol v4) is the most likely to be missing these guards.

---

## Test
The test update in `InsertUpdateIfConditionTest` corrects UDT helper call syntax and re-verifies the null/unset IN-list behaviour introduced by the parent commit.
