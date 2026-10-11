# Bug 0340: Delete file writer uses data file format instead of delete file format

**Commit**: ad72516922f5b56133d3ab930ea82fa369369cdb · **Ticket**: #15356

## Detection Rule

When a class stores multiple format/type fields from a constructor that accepts both (e.g., `dataFileFormat` and `deleteFileFormat`), verify that each stored field is assigned from the correct parameter and not from the wrong one.

## Code Smell

A field named `format` (or similarly generic) assigned from `dataFileFormat` in a class that also receives `deleteFileFormat`; the field is then used to dispatch delete-file writer creation via a `switch` statement.

## Tags

- scope: universal
- category: wrong-constant
- code-pattern: wrong-constructor-param-assigned
- severity: silent-wrong-result
- subsystem: spark/file-writer-factory

## Root Cause

`SparkFileWriterFactory` accepted both `dataFileFormat` and `deleteFileFormat` constructor parameters but stored `dataFileFormat` into the `format` field in both constructors. The `format` field was then used when creating position delete writers, causing delete files to be written in the data file format rather than the configured delete file format. An unrelated secondary bug also called `.metricsConfig(metricsConfig)` twice in the Parquet position delete writer builder chain, applying metrics config redundantly.

## Example

```java
// BEFORE
private final FileFormat format;  // field name gives no hint which format

// in constructor:
this.format = dataFileFormat;   // BUG: should use deleteFileFormat

// in createPositionDeleteWriter():
return switch (format) { ... }
```

```java
// AFTER
private final FileFormat deleteFormat;

// in constructor:
this.deleteFormat = deleteFileFormat;   // correct parameter

// in createPositionDeleteWriter():
return switch (deleteFormat) { ... }
```
