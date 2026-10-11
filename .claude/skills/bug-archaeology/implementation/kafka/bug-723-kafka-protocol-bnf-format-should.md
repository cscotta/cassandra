**Commit**: 33cd114375 · **Ticket**: KAFKA-9825
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
# Description
The [protocol guide](https://kafka.apache.org/protocol) 1) doesn't
display
tagged fields in BNF, and 2) includes empty tagged fields and redundant
nested tables in tables. # Change
## BNF
Now tagged fields are displayed as FIELD_NAME<tag number>

Old:  <img width="1316" height="275" alt="Screenshot 2025-09-13 at 5 34
28 PM"

src="https://github.com/user-attachments/assets/c3e59382-7a6b-43f3-bc7a-893fb27d524d"
/>

New:  <img width="1386" height="328" alt="Screenshot 2025-09-24 at 12 50
34 PM"

src="https://github.com/user-attachments/assets/1ddbc95e-b0a7-4cd5-a5e0-e1303ffd2d06"
/>

Array Field: <img width="914" height="275" alt="Screenshot 2025-09-24 at
12 52 19 PM"

src="https:...

**Before/After**
```java
// Before
                b.append("_tagged_fields ");
            b.append("<td>");
            b.append(field.def.name);
            b.append("</td>");
            b.append("<td>");
                    b.append("<table class=\"data-table\"><tbody>\n");
                    b.append("<tr>");
                    b.append("<th>Tag</th>\n");
                    b.append("<th>Tagged field</th>\n");
                    b.append("<th>Description</th>\n");
                    b.append("</tr>");
                        b.append("<tr>\n");

// After
import org.apache.kafka.common.protocol.types.Field;
import java.util.TreeMap;
                Map<Integer, Field> taggedFields = new TreeMap<>(((TaggedFields) type).fields());
                taggedFields.forEach((tag, taggedField) -> {
                    if (taggedField.type.isArray()) {
                        b.append("[");
                        b.append(taggedField.name);
                        b.append("]");
                        if (!subTypes.containsKey(taggedField.name))
                            subTypes.put(taggedField.name + "&lt;tag: " + tag.toString() + "&gt;", taggedField.type.arrayElementType().get());
                    } else {
                        b.append(taggedField.name);
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-9825 kafka protocol bnf format should have some way to display tagged fields (#20536).

**Test**
No test added.
