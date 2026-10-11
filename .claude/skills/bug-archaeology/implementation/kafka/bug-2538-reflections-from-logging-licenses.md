**Commit**: b40b5a24f4 · **Ticket**: KAFKA-17369
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
reflections-0.10.2

Do What The F*ck You Want To Public License
see: licenses/DWTFYWTPL

reflections-0.10.2

log4j.logger.org.reflections=ERROR

# for connect-plugin-path
log4j.logger.org.reflections=ERROR
log4j.logger.org.reflections=ERROR

// After
classgraph-4.8.173, see: licenses/classgraph-MIT
The MIT License (MIT)

Copyright (c) 2019 Luke Hutchison

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17369: remove reflections from logging and update licenses (#16924).

**Test**
Test modified in log4j.properties covering the corrected behaviour.
