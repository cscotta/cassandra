**Commit**: baa152ced7 · **Ticket**: KAFKA-18195
**Subsystem**: docs/streams/upgrade-guide.html
**Category**: Wrong constant / default value

**Root cause**
The Kafka Streams broker compatibility matrix in the documentation listed many legacy broker versions (0.10.0.x through 2.3.x) that Kafka Streams 4.0 no longer supports, along with blank/empty cells instead of "incompatible" for unsupported combinations. This misled users into believing broad compatibility still existed. The fix removes the stale rows and columns (everything older than 2.4.x is redirected to the 3.9 upgrade guide) and trims the table to only list the currently supported broker ranges.

**Before/After**
This is a documentation-only change. The table previously had five broker-version columns starting from 0.10.0.x; after the fix it has two columns (2.4.x–2.5.x and 2.6.x–4.0.x).

**How to spot**
Documentation tables with blank cells where "incompatible" should appear silently imply compatibility. Any compatibility matrix that uses empty cells rather than explicit "incompatible" labels is prone to misinterpretation.

**Test**
No test added; documentation-only change.
