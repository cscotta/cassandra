**Commit**: af5df59d2b · **Ticket**: KAFKA-17593
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
This patch is the first of a series of patches to introduce support for server side regular expression. It introduces the re2j dependency. Co-authored-by: Lianet Magrans <lmagrans@confluent.io>

Reviewers: Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After

---------------------------------------
Go License

re2j-1.7 see: licenses/re2j-GO
    implementation libs.re2j
  re2j: "1.7",
  re2j: "com.google.re2j:re2j:$versions.re2j",
This is a work derived from Russ Cox's RE2 in Go, whose license
http://golang.org/LICENSE is as follows:

Copyright (c) 2009 The Go Authors. All rights reserved.
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17593; [1/n] introduce re2j dependency (#17634).

**Test**
No test added.
