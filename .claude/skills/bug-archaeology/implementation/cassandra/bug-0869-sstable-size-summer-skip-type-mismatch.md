# Bug 0869: SSTableSizeSummer Skips No Files Because toSkip Contains File Objects Not Names

**Commit**: 91392edbe8 · **Ticket**: n/a
**Subsystem**: Storage / Directories / SSTableSizeSummer
**Category**: Logic error in condition

---

## Root Cause
`SSTableSizeSummer.isAcceptable` checked whether `toSkip.contains(fileName)` where `fileName` was a `String` (just the base name from `file.toFile().getName()`). However, `toSkip` was declared as a `Set<File>` containing `File` objects, not `String` objects. Because `String.equals(File)` is always `false`, the `contains` call never matched any entry in `toSkip`, so every file that should have been skipped was instead included in the size calculation. The fix compares `File` to `File`: `!toSkip.contains(file)` where `file` is the `File` object constructed from the path.

### Before / After
```java
// BEFORE
public boolean isAcceptable(Path file)
{
    String fileName = file.toFile().getName();
    Pair<Descriptor, Component> pair = SSTable.tryComponentFromFilename(file.getParent().toFile(), fileName);
    return pair != null
            && pair.left.ksname.equals(metadata.ksName)
            && pair.left.cfname.equals(metadata.cfName)
            && !toSkip.contains(fileName);   // String vs Set<File> — never matches
}

// AFTER
public boolean isAcceptable(Path path)
{
    File file = path.toFile();
    Pair<Descriptor, Component> pair = SSTable.tryComponentFromFilename(path.getParent().toFile(), file.getName());
    return pair != null
            && pair.left.ksname.equals(metadata.ksName)
            && pair.left.cfname.equals(metadata.cfName)
            && !toSkip.contains(file);       // File vs Set<File> — correct
}
```

---

## How to Spot
Whenever `Collection.contains` is called with a value whose static type differs from the collection's element type, the check will silently always return `false` (Java generics are erased, so no compile-time error). Review all `contains`/`remove` calls on typed collections and verify the argument type matches. The symptom is that the filter has no effect — here, disk-space calculations would include files that were supposed to be excluded.

---

## Test
No test added.
