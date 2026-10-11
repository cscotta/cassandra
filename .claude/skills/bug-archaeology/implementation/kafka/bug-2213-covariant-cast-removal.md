**Commit**: d55b4e640c · **Ticket**: KAFKA-17763
**Subsystem**: common (Utils.covariantCast)
**Category**: Logic error in condition

**Root cause**
`Utils.covariantCast(Iterator<? extends S>)` was a helper that cast a covariant iterator to `Iterator<S>` using an unchecked cast. In Java 10+ the same result is achieved with `var` local type inference, which lets the compiler infer the precise type without a cast. Keeping the helper added noise and a suppressed unchecked-cast warning that could shadow legitimate warnings.

**Before/After**
```java
// Before
Iterator<Record> iter = Utils.covariantCast(snapshot.records().iterator());

// After
var iter = snapshot.records().iterator();
```

**How to spot**
Search for `Utils.covariantCast` or methods returning unchecked-cast results; when the call site is inside a method where `var` can infer the correct type, remove the helper and replace with `var`.

**Test**
No test added.
