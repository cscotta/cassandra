# Apache Cassandra "BIG" SSTable Format Specification

This document provides a byte-level specification of the "BIG" SSTable format used by Apache Cassandra.
It is intended to serve as a developer reference for understanding, debugging, and implementing SSTable
readers and writers.

> **Tip:** Class references use abbreviated paths under `o.a.c` for `org.apache.cassandra`. Method
> signatures are simplified for clarity.

---

## Table of Contents

1. [Overview](#1-overview)
2. [SSTable Naming Convention](#2-sstable-naming-convention)
3. [SSTable Components Overview](#3-sstable-components-overview)
4. [Read Path Walkthrough](#4-read-path-walkthrough)
5. [Type Encoding Primitives](#5-type-encoding-primitives)
   - 5.1 [VInt Encoding](#51-vint-encoding)
   - 5.2 [DeletionTime Serialization](#52-deletiontime-serialization)
   - 5.3 [EncodingStats and Delta Encoding](#53-encodingstats-and-delta-encoding)
   - 5.4 [Composite Key Encoding](#54-composite-key-encoding)
   - 5.5 [ClusteringPrefix Serialization](#55-clusteringprefix-serialization)
6. [Data File (Data.db)](#6-data-file-datadb)
   - 6.1 [Overall Structure](#61-overall-structure)
   - 6.2 [Compressed vs Uncompressed](#62-compressed-vs-uncompressed)
   - 6.3 [Partition Layout](#63-partition-layout)
   - 6.4 [Row Serialization](#64-row-serialization)
   - 6.5 [Cell Serialization](#65-cell-serialization)
   - 6.6 [Range Tombstone Markers](#66-range-tombstone-markers)
7. [Primary Index File (Index.db)](#7-primary-index-file-indexdb)
   - 7.1 [Overall Structure](#71-overall-structure)
   - 7.2 [RowIndexEntry](#72-rowindexentry)
   - 7.3 [IndexInfo (Column Index Samples)](#73-indexinfo-column-index-samples)
   - 7.4 [Key Cache and Index Entry Variants](#74-key-cache-and-index-entry-variants)
8. [Summary File (Summary.db)](#8-summary-file-summarydb)
9. [Filter File (Filter.db)](#9-filter-file-filterdb)
10. [Statistics File (Statistics.db)](#10-statistics-file-statisticsdb)
11. [CompressionInfo File (CompressionInfo.db)](#11-compressioninfo-file-compressioninfodb)
12. [TOC, Digest, and CRC Files](#12-toc-digest-and-crc-files)
13. [Version History](#13-version-history)
14. [Appendix A: Complete Flag Bit Reference](#appendix-a-complete-flag-bit-reference)

---

## 1. Overview

The **BIG** format is the legacy bigtable SSTable format in Apache Cassandra. It stores partition data
on disk as a collection of component files, each serving a distinct purpose. An SSTable is immutable
once written and is produced during memtable flushes, compaction, streaming, and repair.

The format is registered as `"big"` via `BigFormat` (`o.a.c.io.sstable.format.big.BigFormat`).

An SSTable consists of the following component files:

- **Data.db** — The partition data: keys, rows, cells, and tombstones, written in token order.
- **Index.db** — Primary index mapping partition keys to their positions in Data.db via `RowIndexEntry` pointers. For wide partitions, entries also contain a column index for seeking to specific clustering ranges.
- **Summary.db** — An in-memory sampling of the primary index (every Nth key), enabling fast binary search to narrow lookups before scanning Index.db.
- **Filter.db** — A bloom filter for negative lookups, allowing reads to skip SSTables that definitely do not contain a given partition key.
- **Statistics.db** — SSTable metadata organized into four typed sections: validation (partitioner, FP chance), compaction (cardinality estimator), stats (timestamps, tombstones, sizes, repair state), and the serialization header (schema types, encoding stats).
- **CompressionInfo.db** — Compressed chunk offset table and compressor parameters, enabling random access into a compressed Data.db. Present only for compressed SSTables.
- **TOC.txt** — A plain text file listing all component filenames belonging to this SSTable.
- **Digest.crc32** — CRC32 checksum of the data file content.
- **CRC.db** — Per-chunk CRC32 checksums for uncompressed data files. Present only for uncompressed SSTables (mutually exclusive with CompressionInfo.db).

**Key classes:**
- `BigFormat` — format registration, version definitions, component sets
- `BigTableWriter` / `BigTableReader` — write/read SSTables
- `BigFormatPartitionWriter` / `SortedTablePartitionWriter` — partition-level writing

---

## 2. SSTable Naming Convention

SSTable filenames follow the pattern:

```
<version>-<id>-big-<Component>.<ext>
```

For example: `oa-1234-big-Data.db`

| Segment | Description |
|---------|-------------|
| `<version>` | Two-letter version string (e.g., `ma`, `nb`, `oa`, `pa`) |
| `<id>` | Generation identifier (numeric or UUID) |
| `big` | Format name |
| `<Component>` | Component type name (e.g., `Data`, `Index`, `Summary`) |
| `<ext>` | File extension (typically `.db`, or no extension for `Digest`) |

**Directory structure:**
```
<data_dir>/<keyspace>/<tableName>-<tableId>/[backups/|snapshots/<tag>/][.<indexName>/]<component>
```

The separator between filename segments is `-` (`Descriptor.FILENAME_SEPARATOR`).

**Reference:** `o.a.c.io.sstable.Descriptor` — `appendFileName()` (line 186)

---

## 3. SSTable Components Overview

The BIG format defines 9 components:

| Component | File | Streamable | Purpose |
|-----------|------|:----------:|---------|
| `DATA` | `Data.db` | Yes | Partition data (keys, rows, cells, tombstones) |
| `PRIMARY_INDEX` | `Index.db` | Yes | Partition key index with `RowIndexEntry` pointers into Data.db |
| `SUMMARY` | `Summary.db` | Yes | In-memory sampling of the Index for fast key lookup |
| `FILTER` | `Filter.db` | Yes | Bloom filter for negative lookups |
| `STATS` | `Statistics.db` | Yes | SSTable metadata (validation, compaction, stats, serialization header) |
| `COMPRESSION_INFO` | `CompressionInfo.db` | Yes | Compressed chunk offset table and compressor parameters |
| `DIGEST` | `Digest.crc32` | No | CRC32 digest of the data file |
| `CRC` | `CRC.db` | No | Per-chunk CRC32 checksums (uncompressed data files only) |
| `TOC` | `TOC.txt` | No | Text file listing all component names |

**Component sets** (defined in `BigFormat.Components`):

| Set | Components |
|-----|-----------|
| `ALL_COMPONENTS` | DATA, PRIMARY_INDEX, STATS, COMPRESSION_INFO, FILTER, SUMMARY, DIGEST, CRC, TOC |
| `PRIMARY_COMPONENTS` | DATA, PRIMARY_INDEX |
| `BATCH_COMPONENTS` | DATA, PRIMARY_INDEX, COMPRESSION_INFO, FILTER, STATS |
| `MUTABLE_COMPONENTS` | STATS, SUMMARY |
| `UPLOAD_COMPONENTS` | DATA, PRIMARY_INDEX, SUMMARY, COMPRESSION_INFO, STATS |
| `GENERATED_ON_LOAD` | FILTER, SUMMARY |

**Reference:** `BigFormat.Components` (lines 174–215)

---

## 4. Read Path Walkthrough

A point lookup for a partition key traverses four components:

**Step 1 — Bloom Filter Check (`Filter.db`):**
The bloom filter is tested for the partition key. If the key is definitely absent, the read
short-circuits immediately. False positives proceed to the next step.

**Step 2 — Index Summary Lookup (`Summary.db`):**
The summary is an in-memory sample of every Nth index entry (controlled by `min_index_interval`,
default 128). A binary search finds the two adjacent summary entries that bracket the target key,
narrowing the search to a small region of the index file.

**Step 3 — Index File Lookup (`Index.db`):**
Within the narrowed region, a sequential scan finds the exact partition key. The associated
`RowIndexEntry` provides the data file position. If the partition is "indexed" (wide partition with
multiple index samples), the `IndexInfo` column index entries enable seeking directly to specific
clustering ranges within the partition.

**Step 4 — Data File Read (`Data.db`):**
The partition is read from the data file starting at the offset from the `RowIndexEntry`. For wide
partitions, the column index (`IndexInfo` entries) allows seeking to the relevant clustering range
without reading the entire partition.

**Key classes:**
- `BigTableReader.getPosition()` — summary + index lookup
- `RowIndexEntry.IndexInfoRetriever` — column index access within wide partitions
- `BigTableScanner` — full SSTable scanning

---

## 5. Type Encoding Primitives

### 5.1 VInt Encoding

Variable-length integer encoding is used extensively throughout the SSTable format. The encoding is
based on the Protocol Buffers varint scheme. The number of leading 1-bits in the first byte indicates
how many additional bytes follow.

**Reference:** `o.a.c.utils.vint.VIntCoding`

#### Unsigned VInt
```
  First Byte        Extra Bytes     Value Range
  ────────────      ───────────     ─────────────────────
  0xxxxxxx          0               0 to 127
  10xxxxxx          1               128 to 16,383
  110xxxxx          2               16,384 to 2,097,151
  1110xxxx          3               2,097,152 to 268,435,455
  11110xxx          4               to 34,359,738,367
  111110xx          5               to 4,398,046,511,103
  1111110x          6               to 562,949,953,421,311
  11111110          7               to 72,057,594,037,927,935
  11111111          8               to 2^63 - 1 (full long)
```

The first byte's value bits are extracted by masking: `firstByte & (0xFF >> extraBytesToRead)`.
Subsequent bytes are read and shifted into the result.


#### Signed VInt (ZigZag Encoding)

Signed integers are first converted to unsigned using ZigZag encoding, then encoded as unsigned VInts:
```
  Encode: unsigned = (signed << 1) ^ (signed >> 63)
  Decode: signed  = (unsigned >>> 1) ^ -(unsigned & 1)
```
This maps small-magnitude signed values to small unsigned values:
`0 → 0, -1 → 1, 1 → 2, -2 → 3, 2 → 4, ...`

**Key methods:**
- `VIntCoding.writeUnsignedVInt(long, DataOutputPlus)` (line 436)
- `VIntCoding.readUnsignedVInt(DataInput)` (line 96)
- `VIntCoding.encodeZigZag64(long)` / `decodeZigZag64(long)` (lines 655, 640)

---

### 5.2 DeletionTime Serialization

`DeletionTime` consists of two fields:
- `markedForDeleteAt` — microsecond timestamp marking when the delete occurred
- `localDeletionTime` — seconds since epoch when the tombstone was created (for GC grace)

There are two serialization formats depending on the SSTable version.

**Reference:** `o.a.c.db.DeletionTime` — `getSerializer(Version)` (line 202)

#### Current Format (version >= `oa`, i.e. Cassandra 5.0+)

Uses a compact encoding with a flag byte. `localDeletionTime` is stored as an unsigned 32-bit integer
(valid until year 2106).

```
  LIVE tombstone (single byte):
  ┌──────────┐
  │    0x80    │    IS_LIVE_DELETION flag
  └──────────┘
  (1 byte total)

  Non-LIVE tombstone:
  ┌────────────────────────────────────────────────
  │ markedForDeleteAt (8 bytes)   │ localDeletionTime (4B)  │
  │ [first byte has MSB=0]        │ [unsigned int32]        │
  └────────────────────────────────────────────────
  (12 bytes total)
```

The first byte of `markedForDeleteAt` always has its MSB clear (since `markedForDeleteAt >= 0` for
valid tombstones), so the IS_LIVE flag (0x80) is distinguishable.

**Reference:** `DeletionTime.Serializer` (lines 216–331)

#### Legacy Format (version < `oa`, i.e. pre-5.0)
```
  ┌────────────────────────────────────────────────────
  │ localDeletionTime (4B)       │ markedForDeleteAt (8 bytes)   │
  │ [signed int32]               │ [long]                        │
  └────────────────────────────────────────────────────
  (12 bytes total, always)
```

LIVE is encoded as `localDeletionTime = Integer.MAX_VALUE` and `markedForDeleteAt = Long.MIN_VALUE`.

**Reference:** `DeletionTime.LegacySerializer` (lines 334–384)

---

### 5.3 EncodingStats and Delta Encoding

The `SerializationHeader` stored in the Statistics file contains `EncodingStats` — minimum values for
timestamps, local deletion times, and TTLs observed across the SSTable. These minimums serve as base
values for delta encoding, producing small values that compress well as VInts.

**Reference:** `o.a.c.db.rows.EncodingStats`

**Epoch constants:**
- `TIMESTAMP_EPOCH` — microseconds since 2015-09-22 00:00:00 UTC (Cassandra Summit 2015)
- `DELETION_TIME_EPOCH` — seconds since 2015-09-22 00:00:00 UTC
- `TTL_EPOCH` — 0

**EncodingStats on-disk format:**
```
  ┌───────────────────────────────┬──────────────────────────────────┬──────────────────────┐
  │ minTimestamp - TIMESTAMP_EPOCH│ minLocalDeletionTime - DT_EPOCH  │ minTTL - TTL_EPOCH   │
  │ (unsigned VInt)               │ (unsigned VInt32)                │ (unsigned VInt32)    │
  └───────────────────────────────┴──────────────────────────────────┴──────────────────────┘
```

**Delta encoding in use:** When writing a timestamp to the data file, the header writes
`actual_timestamp - minTimestamp` as a VInt. On read, `readVInt() + minTimestamp` recovers the value.
The same pattern applies to local deletion times and TTLs.

**Key methods:**
- `SerializationHeader.writeTimestamp(long, DataOutputPlus)` — delta-encodes timestamps
- `SerializationHeader.writeTTL(int, DataOutputPlus)` — delta-encodes TTLs
- `SerializationHeader.writeLocalDeletionTime(long, DataOutputPlus)` — delta-encodes deletion times
- `SerializationHeader.writeDeletionTime(DeletionTime, DataOutputPlus)` — writes full DeletionTime using delta encoding

**Reference:** `o.a.c.db.SerializationHeader`, `EncodingStats.Serializer` (lines 286–309)

---

### 5.4 Composite Key Encoding

Composite partition keys (tables with multiple partition key columns) are encoded as a single
`ByteBuffer` using the `CompositeType` encoding scheme.

Each component is encoded as:

```
  ┌──────────────────┬──────────────────┬─────────────────────┐
  │ length (2 bytes) │ value (N bytes)  │ end-of-component    │
  │ [big-endian      │                  │ (1 byte: 0x00)      │
  │  unsigned short] │                  │                     │
  └──────────────────┴──────────────────┴─────────────────────┘
```

Multiple components are concatenated. The end-of-component byte is `0x00` for equality, `0x01` for
"greater than" (end-of-range), and `0xFF` (-1) for "less than" (start-of-range).

For non-composite keys (single partition key column), the key is stored as a raw `ByteBuffer` without
any composite encoding.

---

### 5.5 ClusteringPrefix Serialization

Clustering values (row clusterings, range tombstone bounds) are serialized with a compact header-based
encoding that avoids redundant size information for fixed-width types.

**Kind byte** (1 byte, the `Kind` enum ordinal):

| Ordinal | Kind | Usage |
|:-------:|------|-------|
| 0 | `EXCL_END_BOUND` | Exclusive end of range tombstone |
| 1 | `INCL_START_BOUND` | Inclusive start of range tombstone |
| 2 | `EXCL_END_INCL_START_BOUNDARY` | Boundary: exclusive end + inclusive start |
| 3 | `STATIC_CLUSTERING` | Never serialized |
| 4 | `CLUSTERING` | Regular row clustering |
| 5 | `INCL_END_EXCL_START_BOUNDARY` | Boundary: inclusive end + exclusive start |
| 6 | `INCL_END_BOUND` | Inclusive end of range tombstone |
| 7 | `EXCL_START_BOUND` | Exclusive start of range tombstone |

**Clustering (row):**
```
  kind   : byte (always 4 = CLUSTERING, written by ClusteringPrefix.Serializer)
  values : clustering values (header + raw bytes, no explicit size — size is
           always equal to the number of clustering columns in the schema)
```

**ClusteringBoundOrBoundary (range tombstone bound):**
```
  kind   : byte (0–2 or 5–7, see table above)
  size   : short (number of clustering values in this bound, may be < full width)
  values : clustering values (header + raw bytes)
```

**Value encoding** (shared by both):

Clustering values are written in micro-batches of up to 32 elements. Each batch has a header VInt
followed by raw value bytes:

```
  For each batch of 32 values:
    header : unsigned VInt   (2 bits per value: null/empty/present)
    For each non-null, non-empty value in the batch:
      value_bytes : raw bytes (length determined by the AbstractType)
```

The 2-bit encoding per value (bits at position `i*2` and `i*2+1`):
- `00` — value is present (raw bytes follow)
- `01` — value is empty (zero-length, no bytes follow)
- `10` — value is null (no bytes follow)

For fixed-width types (e.g., `Int32Type`, `LongType`), no explicit length prefix is needed since the
deserializer knows the expected byte count from the type. For variable-width types, the type's
`readValue`/`readArray` method reads a VInt-prefixed length.

**Reference:** `o.a.c.db.ClusteringPrefix.Serializer` (lines 455–601),
`o.a.c.db.Clustering.Serializer`, `o.a.c.db.ClusteringBoundOrBoundary.Serializer`

---

## 6. Data File (Data.db)

### 6.1 Overall Structure

The Data.db file is a sequence of serialized partitions, written in token order:

```
  ┌────────────┬────────────┬─────┬────────────┐
  │ Partition 0│ Partition 1│ ... │ Partition N│
  └────────────┴────────────┴─────┴────────────┘
```

**Reference:**
- `BigTableWriter` (`o.a.c.io.sstable.format.big.BigTableWriter`)
- `SortedTablePartitionWriter.start()` (line 97)

---

### 6.2 Compressed vs Uncompressed

Data can be written in two modes:

**Compressed** (`CompressedSequentialWriter`):
- Data is divided into fixed-size chunks (default 64 KiB, configurable via `chunk_length_in_kb`)
- Each chunk is independently compressed (e.g., LZ4, Zstd, Snappy, Deflate)
- Chunk offsets are stored in `CompressionInfo.db` for random access
- A `Digest.crc32` file stores the overall CRC32 of the compressed data

**Uncompressed** (`ChecksummedSequentialWriter`):
- Data is written with per-chunk CRC32 checksums
- Checksums are stored in `CRC.db`
- A `Digest.crc32` file stores the overall CRC32

Both modes enable random access at the chunk/block level. Only one of `CompressionInfo.db` or `CRC.db`
will exist for a given SSTable, never both.

---

### 6.3 Partition Layout

Each partition in Data.db has the following structure:

```
  ┌─────────────────────────────────────────────────────────────────┐
  │                         Partition                               │
  ├─────────────────────────────────────────────────────────────────┤
  │ key_length : short (2 bytes, big-endian unsigned)               │
  │ key        : byte[key_length]                                   │
  │ partition_deletion : DeletionTime (see §5.2)                    │
  │ [static_row] : Row (present only if table has static columns)   │
  │ unfiltered_0 : Row or RangeTombstoneMarker                     │
  │ unfiltered_1 : Row or RangeTombstoneMarker                     │
  │ ...                                                             │
  │ unfiltered_N : Row or RangeTombstoneMarker                     │
  │ END_OF_PARTITION : 0x01 (1 byte)                                │
  └─────────────────────────────────────────────────────────────────┘
```

The key is written with `ByteBufferUtil.writeWithShortLength()`. The partition-level deletion is
serialized using the version-appropriate `DeletionTime` serializer. If the table schema has static
columns, a static row is always written (it may be empty).

**Reference:** `SortedTablePartitionWriter.start()` (line 97), `.addStaticRow()` (line 117),
`.addUnfiltered()` (line 128), `.finish()` (line 156)

---

### 6.4 Row Serialization

Each row (non-marker unfiltered) is serialized as:

```
  ┌───────┬──────────────┬────────────┬───────┬───────────┬──────────┬─────────┬──────────────┐
  │ flags │[ext_flags]   │[clustering]│[sizes]│[pk_liven.]│[deletion]│[columns]│ columns_data │
  │ (1B)  │(1B, optional)│            │       │           │          │         │              │
  └───────┴──────────────┴────────────┴───────┴───────────┴──────────┴─────────┴──────────────┘
```

#### Flags Byte (1 byte)

| Bit | Mask | Name | Meaning |
|:---:|:----:|------|---------|
| 0 | `0x01` | `END_OF_PARTITION` | End-of-partition marker. Nothing follows. |
| 1 | `0x02` | `IS_MARKER` | Range tombstone marker (not a row) |
| 2 | `0x04` | `HAS_TIMESTAMP` | Row has a primary key liveness timestamp |
| 3 | `0x08` | `HAS_TTL` | Row has TTL/expiration info |
| 4 | `0x10` | `HAS_DELETION` | Row has a row-level deletion |
| 5 | `0x20` | `HAS_ALL_COLUMNS` | Row contains all columns from the header |
| 6 | `0x40` | `HAS_COMPLEX_DELETION` | At least one complex column has a deletion |
| 7 | `0x80` | `EXTENSION_FLAG` | Extended flags byte follows |

#### Extended Flags Byte (1 byte, present only if `EXTENSION_FLAG` is set)

| Bit | Mask | Name | Meaning |
|:---:|:----:|------|---------|
| 0 | `0x01` | `IS_STATIC` | This is a static row |
| 1 | `0x02` | `HAS_SHADOWABLE_DELETION` | Row deletion is shadowable (deprecated since 4.0) |

#### Conditional Fields

1. **Clustering** — absent for static rows. Serialized by `Clustering.serializer.serialize()`.
2. **Sizes** (SSTable only) — two unsigned VInts:
   - `rowBodySize`: size of the remaining row body in bytes (for forward skipping)
   - `previousUnfilteredSize`: size of the previous unfiltered (for reverse queries)
3. **PK Liveness** — present if `HAS_TIMESTAMP`:
   - `timestamp`: delta-encoded VInt (via `header.writeTimestamp()`)
   - If `HAS_TTL`: `ttl` (delta-encoded VInt) + `localExpirationTime` (delta-encoded VInt)
4. **Deletion** — present if `HAS_DELETION`:
   - `DeletionTime` written via `header.writeDeletionTime()` (delta-encoded)
5. **Columns** — present if NOT `HAS_ALL_COLUMNS`:
   - Column subset encoded by `Columns.serializer.serializeSubset()`
6. **Columns Data** — for each column present in the row:
   - Simple column: one `Cell` (see §6.5)
   - Complex column: `[complexDeletion]` (if `HAS_COMPLEX_DELETION`) + `cellCount` (unsigned VInt32) + `Cell[]`

**Reference:** `o.a.c.db.rows.UnfilteredSerializer` (lines 46–262)

---

### 6.5 Cell Serialization

Each cell is serialized as:

```
  ┌───────┬───────────┬───────────────┬──────┬───────────┬──────┬────────────┬───────┐
  │ flags │[timestamp]│[deletion_time]│[ttl] │[path_size]│[path]│[value_size]│[value]│
  │ (1B)  │(VInt)     │(VInt)         │(VInt)│(VInt)     │      │(VInt)      │       │
  └───────┴───────────┴───────────────┴──────┴───────────┴──────┴────────────┴───────┘
```

#### Cell Flags Byte (1 byte)

| Bit | Mask | Name | Meaning |
|:---:|:----:|------|---------|
| 0 | `0x01` | `IS_DELETED` | Cell is a tombstone |
| 1 | `0x02` | `IS_EXPIRING` | Cell has a TTL |
| 2 | `0x04` | `HAS_EMPTY_VALUE` | Cell value is empty (common for tombstones) |
| 3 | `0x08` | `USE_ROW_TIMESTAMP` | Cell uses the row's timestamp (not stored separately) |
| 4 | `0x10` | `USE_ROW_TTL` | Cell uses the row's TTL and local deletion time |

#### Conditional Fields

- **Timestamp**: absent if `USE_ROW_TIMESTAMP`. Delta-encoded via `header.writeTimestamp()`.
- **Deletion time**: present if (`IS_DELETED` or `IS_EXPIRING`) and not `USE_ROW_TTL`. Delta-encoded.
- **TTL**: present if `IS_EXPIRING` and not `USE_ROW_TTL`. Delta-encoded.
- **Path**: present only for complex columns (collections, UDTs). Path size + path bytes. For:
  - **Lists**: TimeUUID path (16 bytes)
  - **Sets**: element value as path
  - **Maps**: key value as path
  - **UDTs**: short field index
- **Value size**: absent if `HAS_EMPTY_VALUE` or if the column type has a fixed length.
- **Value**: absent if `HAS_EMPTY_VALUE`. Written by `AbstractType.writeValue()`.

**Frozen collections** are serialized as a single cell value (not multi-cell):
- List/Set: `[count:int32][elem_size:int32][elem_bytes]...`
- Map: `[count:int32][key_size:int32][key][val_size:int32][val]...`

**Reference:** `o.a.c.db.rows.Cell.Serializer` (lines 277–419)

---

### 6.6 Range Tombstone Markers

Range tombstone markers delimit ranges of deleted clusterings within a partition. They are
serialized as:

```
  ┌────────────┬────────────────────┬───────┬──────────────────────────┐
  │ flags=0x02 │ bound_or_boundary  │[sizes]│ deletion_time(s)         │
  │ IS_MARKER  │ (ClusteringBound   │       │                          │
  │            │  serialized)       │       │                          │
  └────────────┴────────────────────┴───────┴──────────────────────────┘
```

For SSTable format, `sizes` includes `markerBodySize` (unsigned VInt) and `previousUnfilteredSize`
(unsigned VInt), similar to rows.

**Bound markers** (open or close a range): one `DeletionTime`.

**Boundary markers** (close one range and open another): two `DeletionTime` values — the end deletion
time first, then the start deletion time.

The clustering bound is serialized with its `Kind` byte:
- `INCL_START_BOUND` (0), `EXCL_END_BOUND` (1), `INCL_END_BOUND` (2), `EXCL_START_BOUND` (3),
  `BOUNDARY` (4, 5 for the two boundary variants)

**Reference:** `UnfilteredSerializer.serialize(RangeTombstoneMarker, ...)` (lines 293–316)

---

## 7. Primary Index File (Index.db)

### 7.1 Overall Structure

The index file contains one entry per partition, in the same order as the data file:

```
  For each partition:
  ┌──────────────────────────────────────────────────────┐
  │ key_length : short (2 bytes, big-endian unsigned)    │
  │ key        : byte[key_length]                        │
  │ RowIndexEntry (serialized, see below)                │
  └──────────────────────────────────────────────────────┘
```

---

### 7.2 RowIndexEntry

The binary format of a `RowIndexEntry` depends on whether the partition has column index samples:

```
  Non-indexed (small partitions, < 2 index samples):
  ┌────────────────────────┬───────────────────┐
  │ position (unsigned VInt)│ 0 (unsigned VInt) │
  └────────────────────────┴───────────────────┘

  Indexed (wide partitions, >= 2 index samples):
  ┌───────────────────────┬──────────────────────┬──────────────────────┐
  │ position (unsign VInt)│ size (unsigned VInt)  │ indexed data...      │
  └───────────────────────┴──────────────────────┴──────────────────────┘
```

Where `position` is the byte offset in Data.db, and `size` is the serialized size of the indexed
data that follows (0 if not indexed).

**Indexed data layout:**

```
  ┌──────────────────────────┐
  │ headerLength (unsign VInt)│  Length of partition header in Data.db
  ├──────────────────────────┤
  │ DeletionTime             │  Partition-level deletion (see §5.2)
  ├──────────────────────────┤
  │ columnIndexCount         │  Number of IndexInfo entries (unsigned VInt)
  │ (unsigned VInt)          │
  ├──────────────────────────┤
  │ IndexInfo[0]             │  First column index sample
  │ IndexInfo[1]             │  Second column index sample
  │ ...                      │
  │ IndexInfo[N-1]           │  Last column index sample
  ├──────────────────────────┤
  │ offset[0] : int32        │  Byte offset of IndexInfo[0] (relative to first IndexInfo)
  │ offset[1] : int32        │  Byte offset of IndexInfo[1]
  │ ...                      │
  │ offset[N-1] : int32      │  Byte offset of IndexInfo[N-1]
  └──────────────────────────┘
```

The offsets table enables binary search within the column index for a specific clustering.

**Reference:** `o.a.c.io.sstable.format.big.RowIndexEntry` (lines 60–101, 277–464)

---

### 7.3 IndexInfo (Column Index Samples)

Each `IndexInfo` entry represents a ~64 KiB portion of partition data in the data file and contains:

```
  ┌───────────────────────────────────────────────────────────────────────────┐
  │ firstName : ClusteringPrefix   (serialized via ClusteringPrefix.serializer)│
  │ lastName  : ClusteringPrefix   (serialized via ClusteringPrefix.serializer)│
  │ offset    : unsigned VInt      (position in data file, relative to        │
  │                                 partition start)                           │
  │ width     : signed VInt        (stored as width - WIDTH_BASE, where       │
  │                                 WIDTH_BASE = 64 * 1024 = 65536)           │
  │ hasEndOpenMarker : boolean     (1 byte)                                   │
  │ [endOpenMarker : DeletionTime] (present only if hasEndOpenMarker=true)    │
  └───────────────────────────────────────────────────────────────────────────┘
```

The `width` is delta-encoded using `WIDTH_BASE = 64 * 1024` to produce small VInt values since most
index blocks are close to 64 KiB.

The `endOpenMarker` records any open range tombstone at the end of this index block, so that a reader
seeking to this block can properly account for the tombstone.

**Reference:** `o.a.c.io.sstable.IndexInfo` (lines 38–167)

---

### 7.4 Key Cache and Index Entry Variants

Cassandra uses three concrete implementations of `RowIndexEntry`:

| Class | Condition | Description |
|-------|-----------|-------------|
| `RowIndexEntry` | `blockCount` <= 1 | Only stores the data file position. No index samples. |
| `IndexedEntry` | `blockCount` > 1 AND serialized size <= `column_index_cache_size` | Index samples kept on-heap in an array. Fast access. |
| `ShallowIndexedEntry` | `blockCount` > 1 AND serialized size > `column_index_cache_size` | Index samples left on disk. Accessed via the offsets table for random access. |

The `column_index_cache_size` configuration parameter (default 8 KiB, configurable via
`column_index_cache_size` in `cassandra.yaml`) controls the threshold.

When a `RowIndexEntry` is stored in the key cache, it is serialized with a type byte:
- `0` — `CACHE_NOT_INDEXED`
- `1` — `CACHE_INDEXED` (IndexedEntry)
- `2` — `CACHE_INDEXED_SHALLOW` (ShallowIndexedEntry)

**Reference:** `RowIndexEntry.create()` (line 221), `IndexedEntry` (line 491), `ShallowIndexedEntry` (line 693)

---

## 8. Summary File (Summary.db)

The Summary file contains an in-memory-optimized sampling of the Index file. It enables a fast
first-pass binary search to narrow down the region of the Index file to scan.

**On-disk format:**

```
  ┌─────────────────────────────────────────────────────────────────┐
  │ minIndexInterval    : int32                                     │
  │ offsetCount         : int32       (number of summary entries)   │
  │ offHeapSize         : int64       (total size of offsets+entries)│
  │ samplingLevel       : int32       (1-128, controls downsampling)│
  │ sizeAtFullSampling  : int32       (number at full sampling)     │
  ├─────────────────────────────────────────────────────────────────┤
  │ Offsets Section (offsetCount * 4 bytes):                        │
  │   offset[0] : int32 (Little Endian)                             │
  │   offset[1] : int32 (Little Endian)                             │
  │   ...                                                           │
  │   offset[N-1] : int32 (Little Endian)                           │
  ├─────────────────────────────────────────────────────────────────┤
  │ Entries Section:                                                │
  │   entry[0] : (DecoratedKey bytes + index file position as long) │
  │   entry[1] : ...                                                │
  │   ...                                                           │
  ├─────────────────────────────────────────────────────────────────┤
  │ firstKey : length-prefixed (int32 length + key bytes)           │
  │ lastKey  : length-prefixed (int32 length + key bytes)           │
  └─────────────────────────────────────────────────────────────────┘
```

**Important notes:**
- The offsets are stored in **Little Endian** byte order (as of Cassandra 5.0, changed from native endian)
- On-disk, offsets are relative to the start of the combined offsets+entries structure. In memory,
  they are rebased to be relative to the entries section start.
- The first/last keys are appended after the IndexSummary data by `IndexSummaryComponent.save()`.

**Reference:**
- `o.a.c.io.sstable.indexsummary.IndexSummary.IndexSummarySerializer` (lines 400–501)
- `o.a.c.io.sstable.format.big.IndexSummaryComponent.save()` (line 118)

---

## 9. Filter File (Filter.db)

The Filter file contains a Bloom filter used for fast negative lookups.

**Reference:** `o.a.c.utils.BloomFilterSerializer`

#### New Format (version >= `na`, i.e. Cassandra 4.0+)

```
  ┌───────────────────────────────────┐
  │ hashCount : int32                 │  Number of hash functions
  ├───────────────────────────────────┤
  │ Bitset:                           │
  │   wordCount : int32               │  Number of 64-bit words (byteSize / 8)
  │   data : byte[wordCount * 8]      │  Raw bytes, sequential copy
  └───────────────────────────────────┘
```

The bitset data is written as a raw byte stream (`Memory.write()`).

#### Old Format (version < `na`, i.e. pre-4.0)

```
  ┌───────────────────────────────────┐
  │ hashCount : int32                 │
  ├───────────────────────────────────┤
  │ Bitset:                           │
  │   wordCount : int32               │  Number of 64-bit words
  │   For each word:                  │
  │     word : int64                  │  Written as big-endian long, but
  │                                   │  bytes within are little-endian ordered
  │                                   │  (byte-swapped on read/write)
  └───────────────────────────────────┘
```

The old format reverses byte order within each 8-byte word during serialization and deserialization.
On read, each long is read big-endian and the individual bytes are extracted in little-endian order.
This relates to CASSANDRA-9067 which changed the bloom filter hash order in 3.0.

**Reference:**
- `BloomFilterSerializer.serialize()` / `.deserialize()` (lines 49–79)
- `OffHeapBitSet.deserialize()` (line 146) — handles old vs new format
- `OffHeapBitSet.serialize()` (line 115) vs `.serializeOldBfFormat()` (line 124)

---

## 10. Statistics File (Statistics.db)

The Statistics file contains SSTable metadata organized into four typed components. For versions
with checksumming (>= `na`), each section is followed by a CRC32 checksum.

**Reference:** `o.a.c.io.sstable.metadata.MetadataSerializer` (lines 53–113)

### File Layout

```
  ┌────────────────────────────────────────────────────────────────────┐
  │ component_count : int32           (number of metadata components)  │
  │ [CRC32 : int32]                   (if version >= "na")            │
  ├────────────────────────────────────────────────────────────────────┤
  │ TOC (Table of Contents):                                          │
  │   For each component:                                              │
  │     type_ordinal : int32          (MetadataType enum ordinal)      │
  │     position     : int32          (byte offset in this file)       │
  │ [CRC32 : int32]                   (if version >= "na")            │
  ├────────────────────────────────────────────────────────────────────┤
  │ Component 0 bytes                                                  │
  │ [CRC32 : int32]                   (if version >= "na")            │
  ├────────────────────────────────────────────────────────────────────┤
  │ Component 1 bytes                                                  │
  │ [CRC32 : int32]                   (if version >= "na")            │
  ├────────────────────────────────────────────────────────────────────┤
  │ ...                                                                │
  └────────────────────────────────────────────────────────────────────┘
```

### MetadataType Enum

| Ordinal | Type | Purpose |
|:-------:|------|---------|
| 0 | `VALIDATION` | Partitioner class name + bloom filter FP chance |
| 1 | `COMPACTION` | Cardinality estimator (HyperLogLogPlus) |
| 2 | `STATS` | Runtime statistics, timestamps, tombstones, etc. |
| 3 | `HEADER` | Serialization header (column types, encoding stats) |

### VALIDATION Metadata

```
  ┌──────────────────────────────────────────────────────┐
  │ partitioner : UTF string (Java modified UTF-8)       │
  │ bloomFilterFPChance : double (8 bytes)               │
  └──────────────────────────────────────────────────────┘
```

**Reference:** `o.a.c.io.sstable.metadata.ValidationMetadata`

### COMPACTION Metadata

```
  ┌──────────────────────────────────────────────────────┐
  │ cardinality_length : int32                           │
  │ cardinality_bytes : byte[cardinality_length]         │
  │   (HyperLogLogPlus serialized estimator)             │
  └──────────────────────────────────────────────────────┘
```

**Reference:** `o.a.c.io.sstable.metadata.CompactionMetadata`

### Embedded Type Formats

Several compound types appear repeatedly in the Statistics file. Their formats are documented here
for reference.

**EstimatedHistogram:**
```
  bucketCount  : int32        (number of buckets)
  For each bucket:
    offset     : int64        (bucket boundary value)
    count      : int64        (number of values in bucket)
```
The `offset` array has `bucketCount - 1` logical entries; the first bucket's offset slot is shared
with index 0. **Reference:** `o.a.c.utils.EstimatedHistogram.EstimatedHistogramSerializer`

**TombstoneHistogram:**
```
  maxBinSize   : int32        (written for legacy compat; equals size)
  size         : int32        (number of entries)
  For each entry:
    [if version >= "oa"]:
      point    : int64        (local deletion time as long)
      count    : int32        (tombstone count)
    [else — legacy]:
      point    : double (8 bytes, cast from deletion time)
      count    : int64  (8 bytes, cast from int count)
```
Version >= `oa` uses `HistogramSerializer`; earlier versions use `LegacyHistogramSerializer` (selected
via `version.hasUIntDeletionTime()`). Both formats have 16 bytes per entry but with different field types.
**Reference:** `o.a.c.utils.streamhist.TombstoneHistogram.HistogramSerializer`

**CommitLogPosition:**
```
  segmentId    : int64        (commit log segment ID)
  position     : int32        (byte offset within segment)
```
**Reference:** `o.a.c.db.commitlog.CommitLogPosition.CommitLogPositionSerializer`

**IntervalSet\<CommitLogPosition\>:**
```
  count        : int32        (number of intervals)
  For each interval:
    start      : CommitLogPosition  (segmentId:int64 + position:int32)
    end        : CommitLogPosition  (segmentId:int64 + position:int32)
```
**Reference:** `o.a.c.db.commitlog.IntervalSet.serializer()`

### STATS Metadata

The STATS component is the largest and most version-sensitive. Fields are serialized in this order:

```
  estimatedPartitionSize     : EstimatedHistogram (see above)
  estimatedCellPerPartitionCount : EstimatedHistogram (see above)
  commitLogUpperBound        : CommitLogPosition (see above)
  minTimestamp               : int64
  maxTimestamp               : int64
  minLocalDeletionTime       : int32 (uint32 if version >= "oa")
  maxLocalDeletionTime       : int32 (uint32 if version >= "oa")
  minTTL                     : int32
  maxTTL                     : int32
  compressionRatio           : double (8 bytes)
  estimatedTombstoneDropTime : TombstoneHistogram (see above)
  sstableLevel               : int32
  repairedAt                 : int64

  [if version.hasLegacyMinMax()]:        (versions "ma"–"nb")
    minClusteringCount       : int32
    minClusteringValues[]    : short-length-prefixed ByteBuffer[]
    maxClusteringCount       : int32
    maxClusteringValues[]    : short-length-prefixed ByteBuffer[]
  [else if version.hasImprovedMinMax()]: (version >= "oa", when no legacy)
    clusteringTypes          : AbstractType list
    coveredClustering        : Slice

  hasLegacyCounterShards     : boolean (1 byte)
  totalColumnsSet            : int64
  totalRows                  : int64

  [if version.hasCommitLogLowerBound()]:  (version >= "mb")
    commitLogLowerBound      : CommitLogPosition (see above)
  [if version.hasCommitLogIntervals()]:   (version >= "mc")
    commitLogIntervals       : IntervalSet<CommitLogPosition> (see above)

  [if version.hasPendingRepair()]:        (version >= "na")
    hasPendingRepair         : byte (0 or 1)
    [if 1]: pendingRepair    : TimeUUID (16 bytes)
  [if version.hasIsTransient()]:          (version >= "na")
    isTransient              : boolean (1 byte)
  [if version.hasOriginatingHostId()]:    (version >= "me" or >= "nb")
    hasHostId                : byte (0 or 1)
    [if 1]: originatingHostId: UUID (16 bytes)
  [if version.hasPartitionLevelDeletionsPresenceMarker()]: (version >= "oa")
    hasPartitionLevelDeletions : boolean (1 byte)

  [if version.hasImprovedMinMax() && version.hasLegacyMinMax()]: (version "oa"+"nb" transitional)
    clusteringTypes          : AbstractType list
    coveredClustering        : Slice

  [if version.hasKeyRange()]:             (version >= "oa")
    firstKey                 : VInt-length-prefixed ByteBuffer
    lastKey                  : VInt-length-prefixed ByteBuffer
  [if version.hasTokenSpaceCoverage()]:   (version >= "oa")
    tokenSpaceCoverage       : double (8 bytes)
```

**Reference:** `o.a.c.io.sstable.metadata.StatsMetadata.StatsMetadataSerializer` (lines 307–682)

### HEADER Metadata (SerializationHeader)

The serialization header stores the schema information needed to deserialize Data.db. It is written
by `SerializationHeader.Serializer.serialize()`.

```
  ┌──────────────────────────────────────────────────────────────────────┐
  │ EncodingStats:                                                       │
  │   minTimestamp          : unsigned VInt (delta from TIMESTAMP_EPOCH)  │
  │   minLocalDeletionTime  : unsigned VInt (delta from DELETION_EPOCH)   │
  │   minTTL                : unsigned VInt (delta from TTL_EPOCH = 0)    │
  ├──────────────────────────────────────────────────────────────────────┤
  │ keyType                 : VInt-length-prefixed UTF-8 type string      │
  ├──────────────────────────────────────────────────────────────────────┤
  │ clusteringTypeCount     : unsigned VInt                               │
  │ For each clustering type:                                             │
  │   clusteringType        : VInt-length-prefixed UTF-8 type string      │
  ├──────────────────────────────────────────────────────────────────────┤
  │ staticColumnCount       : unsigned VInt                               │
  │ For each static column:                                               │
  │   columnName            : VInt-length-prefixed bytes                  │
  │   columnType            : VInt-length-prefixed UTF-8 type string      │
  ├──────────────────────────────────────────────────────────────────────┤
  │ regularColumnCount      : unsigned VInt                               │
  │ For each regular column:                                              │
  │   columnName            : VInt-length-prefixed bytes                  │
  │   columnType            : VInt-length-prefixed UTF-8 type string      │
  └──────────────────────────────────────────────────────────────────────┘
```

Type strings are the Java class names of `AbstractType` instances (e.g., `org.apache.cassandra.db.marshal.Int32Type`),
serialized as UTF-8 via `AbstractTypeSerializer`. Each type string (and column name) is written as a
VInt-encoded byte length followed by the raw bytes.

**Reference:** `o.a.c.db.SerializationHeader.Serializer` (lines 594–617),
`o.a.c.serializers.AbstractTypeSerializer`

---

## 11. CompressionInfo File (CompressionInfo.db)

Present only for compressed SSTables. Contains the compressor parameters and a table of offsets for
each compressed chunk.

**Reference:** `o.a.c.io.compress.CompressionMetadata.open()` (line 77)

```
  ┌──────────────────────────────────────────────────────────────────────┐
  │ compressorName    : UTF string (Java modified UTF-8)                │
  │ optionCount       : int32                                           │
  │ For each option:                                                     │
  │   key             : UTF string                                      │
  │   value           : UTF string                                      │
  │ chunkLength       : int32        (uncompressed chunk size in bytes)  │
  │ [maxCompressedSize: int32]       (version >= "na" only)             │
  │ dataLength        : int64        (total uncompressed data size)      │
  ├──────────────────────────────────────────────────────────────────────┤
  │ Chunk Offsets:                                                       │
  │   chunkCount      : int32        (number of compressed chunks)      │
  │   For each chunk:                                                    │
  │     offset        : int64        (position in compressed Data.db)   │
  ├──────────────────────────────────────────────────────────────────────┤
  │ [Compression Dictionary] (version >= "pa" only):                    │
  │   Deserialized by CompressionDictionary.deserialize()               │
  └──────────────────────────────────────────────────────────────────────┘
```

Chunk offsets allow random access: to read uncompressed byte `N`, compute `chunkIndex = N / chunkLength`,
look up `offset[chunkIndex]` and `offset[chunkIndex + 1]` to determine the compressed chunk boundaries,
decompress that chunk, and extract the desired bytes.

---

## 12. TOC, Digest, and CRC Files

### TOC.txt

A plain text file listing all component filenames, one per line, in lexicographic order:

```
CRC.db
CompressionInfo.db
Data.db
Digest.crc32
Filter.db
Index.db
Statistics.db
Summary.db
TOC.txt
```

Written/updated by `TOCComponent.updateTOC()`. If missing, it can be regenerated by discovering
component files on disk.

**Reference:** `o.a.c.io.sstable.format.TOCComponent`

### Digest (Digest.crc32)

Contains the CRC32 checksum of the data file content, written as a text representation of the integer.

### CRC.db

Present only for **uncompressed** data files. Contains per-chunk CRC32 checksums written by
`ChecksumWriter`. The chunk size matches the configured block size (default 64 KiB).

---

## 13. Version History

SSTable versions use a two-character identifier: a major letter (series) and a minor letter. The
major letter corresponds to a Cassandra major version series.

### Version Table

| Version | Cassandra | Key Changes |
|---------|-----------|-------------|
| **ma** | 3.0.0 | Swap BF hash order; store rows natively (post-CASSANDRA-8099 format) |
| **mb** | 3.0.7 | Commit log lower bound in stats |
| **mc** | 3.0.8 | Commit log intervals in stats |
| **md** | 3.0.18 | Corrected SSTable min/max clustering |
| **me** | 3.0.25 | Added originating host ID |
| **na** | 4.0-rc1 | Uncompressed chunks, pending repair, isTransient, metadata checksums, new BF format |
| **nb** | 4.0-rc2 | Originating host ID (re-added for N series) |
| **oa** | 5.0 | Improved min/max, partition deletion marker, key range, uint deletion time, token coverage |
| **pa** | 6.0 | Compression dictionary metadata in CompressionInfo |

**Earliest supported version:** `ma`

### Feature Flags Matrix

| Feature Flag | ma | mb | mc | md | me | na | nb | oa | pa |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `hasCommitLogLowerBound` | | X | X | X | X | X | X | X | X |
| `hasCommitLogIntervals` | | | X | X | X | X | X | X | X |
| `hasAccurateMinMax` | | | | X | X | X | X | | |
| `hasLegacyMinMax` | X | X | X | X | X | X | X | | |
| `hasOriginatingHostId` | | | | | X | | X | X | X |
| `hasMaxCompressedLength` | | | | | | X | X | X | X |
| `hasPendingRepair` | | | | | | X | X | X | X |
| `hasIsTransient` | | | | | | X | X | X | X |
| `hasMetadataChecksum` | | | | | | X | X | X | X |
| `hasOldBfFormat` | X | X | X | X | X | | | | |
| `hasImprovedMinMax` | | | | | | | | X | X |
| `hasPartitionLevelDeletionPresenceMarker` | | | | | | | | X | X |
| `hasKeyRange` | | | | | | | | X | X |
| `hasUIntDeletionTime` | | | | | | | | X | X |
| `hasTokenSpaceCoverage` | | | | | | | | X | X |

### Current Version Selection

The default version for new SSTables is selected based on `storage_compatibility_mode`:

```java
// BigFormat.BigVersion (line 436)
current_version = storageCompatibilityMode.isBefore(5) ? "nb" :
                  storageCompatibilityMode.isBefore(6) ? "oa" : "pa";
```

| Compatibility Mode | Default Version |
|---|---|
| CASSANDRA_4 | `nb` |
| CASSANDRA_5 | `oa` |
| CASSANDRA_6 / NONE | `pa` |

### Compatibility Rules

- **`isCompatible()`**: version >= `ma` AND major letter <= current major letter
- **`isCompatibleForStreaming()`**: compatible AND same major letter as current

### Messaging Version Mapping

- Versions >= `oa`: `MessagingService.VERSION_50`
- Versions < `oa`: `MessagingService.VERSION_30`

**Reference:** `BigFormat.BigVersion` (lines 434–621)

---

## Appendix A: Complete Flag Bit Reference

### Unfiltered Flags (1 byte)

```
  Bit 7   Bit 6   Bit 5   Bit 4   Bit 3   Bit 2   Bit 1   Bit 0
  ┌───────┬───────┬───────┬───────┬───────┬───────┬───────┬───────┐
  │  EXT  │CMPLX_ │ALL_   │ HAS_  │ HAS_  │ HAS_  │ IS_   │ END_  │
  │ FLAG  │DELETE │COLUMNS│DELETE │  TTL  │TIMEST.│MARKER │OF_PART│
  │ 0x80  │ 0x40  │ 0x20  │ 0x10  │ 0x08  │ 0x04  │ 0x02  │ 0x01  │
  └───────┴───────┴───────┴───────┴───────┴───────┴───────┴───────┘
```

**Reference:** `UnfilteredSerializer` (lines 111–118)

### Extended Flags (1 byte, present only when `EXTENSION_FLAG` is set)

```
  Bit 7   Bit 6   Bit 5   Bit 4   Bit 3   Bit 2   Bit 1   Bit 0
  ┌───────┬───────┬───────┬───────┬───────┬───────┬───────┬───────┐
  │ (unused)                                      │SHADOW │STATIC │
  │                                               │DELETE │       │
  │                                               │ 0x02  │ 0x01  │
  └───────┴───────┴───────┴───────┴───────┴───────┴───────┴───────┘
```

**Reference:** `UnfilteredSerializer` (lines 123–131)

### Cell Flags (1 byte)

```
  Bit 7   Bit 6   Bit 5   Bit 4   Bit 3   Bit 2   Bit 1   Bit 0
  ┌───────┬───────┬───────┬───────┬───────┬───────┬───────┬───────┐
  │ (unused)              │USE_   │USE_ROW│EMPTY  │EXPIR- │DELETE │
  │                       │ROW_TTL│TIMEST.│VALUE  │  ING  │       │
  │                       │ 0x10  │ 0x08  │ 0x04  │ 0x02  │ 0x01  │
  └───────┴───────┴───────┴───────┴───────┴───────┴───────┴───────┘
```

**Reference:** `Cell.Serializer` (lines 279–283)

### DeletionTime Flags (version >= `oa`)

```
  First byte of DeletionTime:
  ┌───────────────────────────────────────────────────────────────────┐
  │ Bit 7 = 1 (0x80): IS_LIVE_DELETION — entire DeletionTime is LIVE│
  │ Bit 7 = 0       : First byte of markedForDeleteAt (8-byte long)  │
  └───────────────────────────────────────────────────────────────────┘
```

Since valid non-LIVE `markedForDeleteAt` values are non-negative longs, their MSB is always 0,
making the IS_LIVE flag unambiguous.

**Reference:** `DeletionTime.Serializer` (lines 216–331)
