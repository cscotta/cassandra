**Commit**: 768e90f667 · **Ticket**: KAFKA-16669
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.Collection;
    public static List<DescribeAclsResource> aclsResources(Collection<AclBinding> acls) {
        Map<ResourcePattern, List<AccessControlEntry>> patternToEntries = new HashMap<>();
            patternToEntries.computeIfAbsent(acl.pattern(), v -> new ArrayList<>()).add(acl.entry());
        for (Entry<ResourcePattern, List<AccessControlEntry>> entry : patternToEntries.entrySet()) {
            List<AclDescription> aclDescriptions = new ArrayList<>();
        val returnedAcls = new util.HashSet[AclBinding]()
        auth.acls(filter).forEach(returnedAcls.add)
            .setResources(DescribeAclsResponse.aclsResources(returnedAcls)),

// After
import java.util.HashSet;
import java.util.Set;
    public static List<DescribeAclsResource> aclsResources(Iterable<AclBinding> acls) {
        Map<ResourcePattern, Set<AccessControlEntry>> patternToEntries = new HashMap<>();
            patternToEntries.computeIfAbsent(acl.pattern(), v -> new HashSet<>()).add(acl.entry());
        for (Entry<ResourcePattern, Set<AccessControlEntry>> entry : patternToEntries.entrySet()) {
            List<AclDescription> aclDescriptions = new ArrayList<>(entry.getValue().size());
            .setResources(DescribeAclsResponse.aclsResources(auth.acls(filter))),
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16669 remove extra collection copy when generating describeaclsresource (#15924).

**Test**
No test added.
