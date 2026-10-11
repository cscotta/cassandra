# Bug 1076: Prevent ALTER TYPE from Creating Circular References

**Commit**: 98c4a7cd02 · **Ticket**: CASSANDRA-10339
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/AlterTypeStatement.java, src/java/org/apache/cassandra/db/marshal/AbstractType.java (and subclasses)
**Category**: Missing null / bounds check

---

## Root Cause

`ALTER TYPE ... ADD` allowed adding a new field whose type directly or transitively referenced the type being altered, producing a circular type graph that could cause infinite loops or stack overflows during serialization, deserialization, or schema introspection. No reachability check existed before the field was appended to the user-defined type. The fix introduces a `references(AbstractType)` method on `AbstractType` (defaulting to equality check) and overrides it in every container type — `CompositeType`, `TupleType`, `ListType`, `SetType`, `MapType`, and `ReversedType` — to perform a deep recursive walk. `AlterTypeStatement` now calls this check before accepting the new field.

### Before / After

```java
// BEFORE (AlterTypeStatement.java)
List<AbstractType<?>> newTypes = new ArrayList<>(toUpdate.size() + 1);
newTypes.addAll(toUpdate.fieldTypes());
newTypes.add(type.prepare(keyspace()).getType());
```

```java
// AFTER (AlterTypeStatement.java)
AbstractType<?> addType = type.prepare(keyspace()).getType();
if (addType.references(toUpdate))
    throw new InvalidRequestException(String.format(
        "Cannot add new field %s of type %s to type %s as this would create a circular reference",
        fieldName, type, name));

List<AbstractType<?>> newTypes = new ArrayList<>(toUpdate.size() + 1);
newTypes.addAll(toUpdate.fieldTypes());
newTypes.add(addType);
```

---

## How to Spot

Any operation that builds or mutates a graph structure (type hierarchies, schema dependencies, configuration references) needs a cycle-detection step. Reviewers should ask: "can this mutation make A contain B which contains A?" and check whether the code guards against it. The absence of a reachability or ancestor check before a structural mutation is a strong signal that cycle creation may be possible.

---

## Test

`UserTypesTest.testCircularReferences` was added, covering direct self-reference, transitive references via collections (`list`, `set`, `map` key and value), and multi-hop transitive chains.
