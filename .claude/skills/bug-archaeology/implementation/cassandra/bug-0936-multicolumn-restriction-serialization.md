# Bug 0936: Intra-node serialization fails for multicolumn restrictions

**Commit**: 11b7dc113f · **Ticket**: CASSANDRA-11196
**Subsystem**: CQL / Query Restrictions
**Category**: Wrong serialization / deserialization

---

## Root Cause
`PrimaryKeyRestrictionSet.appendBoundTo()` builds composite bounds for multi-column slice restrictions and calls `filterAndSort()`, which uses a `TreeSet` ordered by `AbstractCType`. Since CASSANDRA-7281, it became possible for some of the resulting composites to be empty. `AbstractCType` always sorts `Composites.EMPTY` before every other composite due to its EOC byte, regardless of what the correct ordering is for the query. The sorted list was therefore wrong in position, and when the empty composite was later consumed by intra-node serialization it was treated as a plain empty `Composite` object rather than the sentinel `Composites.EMPTY`, corrupting the serialized form.

### Before / After
```java
// BEFORE
return filterAndSort(setEocs(r, bound, builder.build()));

// AFTER
List<Composite> composites = filterAndSort(setEocs(r, bound, builder.build()));
return Lists.transform(composites, new com.google.common.base.Function<Composite, Composite>()
{
    @Override
    public Composite apply(Composite composite)
    {
        return composite.isEmpty() ? Composites.EMPTY : composite;
    }
});
```

---

## How to Spot
Any code that collects `Composite` objects into a sorted structure and then passes them to serialization should be checked: an empty composite placed in a sorted collection may land in the wrong position (because `Composites.EMPTY`'s EOC is special), and when it resurfaces it must be the canonical `Composites.EMPTY` sentinel, not a plain empty buffer. Look for calls to `filterAndSort` or any `TreeSet<Composite>` whose elements are later handed to intra-node messaging without re-normalizing empty values.

---

## Test
`PrimaryKeyRestrictionSetTest` was updated to cover the multi-column restriction case but the core assertion is exercised through integration-level query tests.
