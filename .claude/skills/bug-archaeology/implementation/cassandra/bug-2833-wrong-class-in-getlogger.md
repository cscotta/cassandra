# Bug 2833: Logger initialized with wrong class in LoggerFactory.getLogger

**Commit**: a7ec06561a · **Ticket**: CASSANDRA-1879

## Detection Rule
If `LoggerFactory.getLogger(X.class)` where `X` is not the enclosing class, log output will appear under the wrong logger name, hiding messages or routing them to wrong appenders.

## Code Smell
`private static Logger logger = LoggerFactory.getLogger(JMXEnabledThreadPoolExecutor.class)` inside `DebuggableThreadPoolExecutor`, and `LoggerFactory.getLogger(StorageService.class)` inside `ColumnFamilyInputFormat`, and `LoggerFactory.getLogger(IndexScanVerbHandler.class)` inside `RangeSliceVerbHandler` — all are copy-paste errors using the wrong class literal.

## Tags
- scope: java
- category: logic-error
- code-pattern: wrong-class-in-getlogger
- severity: cosmetic
- subsystem: n/a

## Root Cause
Three classes copy-pasted logger declarations without updating the class literal passed to `LoggerFactory.getLogger`. Log messages from these classes appear under the source class's logger instead of their own, making filtering and log-level configuration ineffective.

## Example
```java
// BEFORE (DebuggableThreadPoolExecutor)
protected static Logger logger =
    LoggerFactory.getLogger(JMXEnabledThreadPoolExecutor.class);

// AFTER
protected static Logger logger =
    LoggerFactory.getLogger(DebuggableThreadPoolExecutor.class);
```
