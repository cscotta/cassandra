**Commit**: d6688f869c · **Ticket**: KAFKA-15983
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
# Description
`kafka-acls.sh` doesn't print message about duplicate authorization. # Changes 
Now the cli searches for existing AclBinding, prints duplicate bindings,
and removes them from the adding list. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            System.out.println("Adding ACLs for resource `" + resource + "`: " + NL + " " + acls.stream().map(a -> "\t" + a).collect(Collectors.joining(NL)) + NL);
            Collection<AclBinding> aclBindings = acls.stream().map(acl -> new AclBinding(resource, acl)).collect(Collectors.toList());
            admin.createAcls(aclBindings).all().get();

// After

            AclBindingFilter filter = new AclBindingFilter(resource.toFilter(), AccessControlEntryFilter.ANY);
            Set<AclBinding> existingBindingsSet = Set.copyOf(admin.describeAcls(filter).values().get());

            List<AclBinding> aclBindings = new ArrayList<>();
            for (AccessControlEntry acl : acls) {
                AclBinding binding = new AclBinding(resource, acl);
                if (existingBindingsSet.contains(binding)) {
                    System.out.println("Acl " + binding + " already exists.");
                } else {
                    aclBindings.add(binding);
                }
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15983 kafka-acls should return authorization already done if repeating work is issued (#20482).

**Test**
Test modified in AclCommandTest.java covering the corrected behaviour.
