**Commit**: e831dfb409 · **Ticket**: KAFKA-17785
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Ken Huang <s7133700@gmail.com>

**Before/After**
```java
// Before
                b.append("TAG_BUFFER ");
            b.append(field.def.docString);

// After
                b.append("_tagged_fields ");
            if (field.def.type instanceof TaggedFields) {
                TaggedFields taggedFields = (TaggedFields) field.def.type;
                // Only include the field in the table if there are actually tags defined
                if (taggedFields.numFields() > 0) {
                    b.append("<table class=\"data-table\"><tbody>\n");
                    b.append("<tr>");
                    b.append("<th>Tag</th>\n");
                    b.append("<th>Tagged field</th>\n");
                    b.append("<th>Description</th>\n");
                    b.append("</tr>");
                    taggedFields.fields().forEach((tag, taggedField) -> {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17785: add tagged field information to protocol docs (#17498).

**Test**
No test added.
