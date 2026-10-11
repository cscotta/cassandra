# Bug 0433: Unnecessary HEAD request issued on every S3 analytics accelerator stream open

**Commit**: f23486ffd7499d2b2f0bfd8ce7552c361fd86796 · **Ticket**: #15116

## Detection Rule

When a stream factory already fetches object metadata lazily on first read, verify that the caller does not also pre-fetch metadata via a synchronous HEAD request before handing the URI to the factory.

## Code Smell

A `getObjectMetadata()` call (which issues an S3 HEAD request) immediately before creating a seekable stream, where the resulting `HeadObjectResponse` is only used to populate an `OpenStreamInformation` object passed to the factory.

## Tags

- scope: project-specific
- category: logic-error
- code-pattern: redundant-remote-call
- severity: performance
- subsystem: aws/s3/analytics-accelerator

## Root Cause

`AnalyticsAcceleratorUtil.newStream()` performed a HEAD request to populate `OpenStreamInformation` with content-length and ETag before calling `factory.createStream(uri, openStreamInfo)`. The analytics accelerator library already retrieves this metadata internally when it needs it, making the explicit pre-fetch redundant and adding a synchronous round-trip for every stream open. The fix removes the HEAD call and uses the no-argument `createStream(uri)` overload.

## Example

```java
// BEFORE
HeadObjectResponse metadata = inputFile.getObjectMetadata();
OpenStreamInformation openStreamInfo = OpenStreamInformation.builder()
    .objectMetadata(ObjectMetadata.builder()
        .contentLength(metadata.contentLength())
        .etag(metadata.eTag())
        .build())
    .build();
S3SeekableInputStream stream = factory.createStream(uri, openStreamInfo);
```

```java
// AFTER
S3SeekableInputStream stream = factory.createStream(uri);
```
