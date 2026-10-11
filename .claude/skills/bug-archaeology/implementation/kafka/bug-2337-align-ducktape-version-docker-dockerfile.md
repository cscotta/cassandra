**Commit**: e146c7c916 · **Ticket**: KAFKA-17520
**Subsystem**: tests/docker
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
RUN pip3 install --upgrade cffi virtualenv pyasn1 boto3 pycrypto pywinrm ipaddress enum34 debugpy && pip3 install --upgrade "ducktape>0.8"
      install_requires=["ducktape==0.8.14", "requests==2.24.0"],

// After
# NOTE: ducktape 0.11.4 requires python3.9
RUN pip3 install --upgrade cffi virtualenv pyasn1 boto3 pycrypto pywinrm ipaddress enum34 debugpy && pip3 install --upgrade "ducktape==0.11.4"
      install_requires=["ducktape==0.11.4", "requests==2.31.0"],
```

**How to spot**
When reviewing tests/docker code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17520 align ducktape version in tests/docker/dockerfile and tests/setup.py (#17240).

**Test**
Test modified in Dockerfile covering the corrected behaviour.
