**Commit**: 52017a83db · **Ticket**: KAFKA-17383
**Subsystem**: docs (upgrade.html)
**Category**: Missing null / bounds check

**Root cause**
The 4.0 upgrade guide omitted mention of the removal of `offsets.commit.required.acks`. Operators upgrading from 3.x would encounter a startup failure or unexpected behaviour without any documented migration path. The design gap is a missing documentation entry that should accompany every config removal.

**Before/After**
```html
<!-- Before – no mention of offsets.commit.required.acks removal -->

<!-- After – explicit note added -->
<li>The <code>offsets.commit.required.acks</code> configuration
has been removed. It was previously deprecated and brokers
will reject any value other than -1 (default).</li>
```

**How to spot**
For every configuration key removed or deprecated in a release, ensure `docs/upgrade.html` contains a corresponding bullet under the relevant version heading before the release is cut.

**Test**
No test added.
