**Commit**: c9fa8c5b3f · **Ticket**: KAFKA-19944

**Subsystem**: tools/src/main/java/org/apache/kafka/tools/consumer/group/ShareGroupCommand.java

**Category**: Silent skip on non-match

**Root cause**
`kafka-share-groups.sh --describe --offsets` printed only column headers when no offset information was available (e.g. after the consumed topic was deleted), making it appear as if the command had produced an empty result set without explanation. The fix adds a guard: if `offsetsInfo` is empty a descriptive message is printed instead of the empty header row, matching the behaviour already implemented for the "no active members" case in the same tool.

**Before**
```java
// Before: always prints header even when list is empty
String fmt = printOffsetFormat(groupId, offsetsInfo, verbose);
System.out.printf(fmt, "GROUP", "TOPIC", ...);
for (SharePartitionOffsetInformation info : offsetsInfo) { ... }
System.out.println();
```

**After**
```java
// After: prints informational message when empty
if (offsetsInfo.isEmpty()) {
    System.out.println("\nShare group '" + groupId +
        "' has no offset information.");
} else {
    String fmt = printOffsetFormat(groupId, offsetsInfo, verbose);
    System.out.printf(fmt, "GROUP", "TOPIC", ...);
    ...
}
```

**How to spot**
Any CLI describe command that calls `printOffsetFormat` without first checking whether the data list is empty will print an orphaned header row; compare with the members-describe path to find asymmetric empty-list handling.

**Test**
Call `ShareGroupCommand --describe --offsets` for a group whose topic has been deleted and assert that the output contains the "has no offset information" message rather than a bare header row.
