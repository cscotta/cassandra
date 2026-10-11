**Commit**: d8bfbb7d1a · **Ticket**: KAFKA-17791
**Subsystem**: tests/docker
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
RUN pip3 install --upgrade cffi virtualenv pyasn1 boto3 pycrypto pywinrm ipaddress enum34 debugpy psutil && pip3 install --upgrade "ducktape==0.12.0"

// After
COPY requirements.txt requirements.txt
RUN pip3 install --upgrade -r requirements.txt
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
```

**How to spot**
When reviewing tests/docker code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17791: dockerfile should use requirements.txt for dependencies (#17542).

**Test**
Test modified in Dockerfile covering the corrected behaviour.
