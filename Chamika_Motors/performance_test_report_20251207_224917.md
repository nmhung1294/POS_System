# Performance Test Report

**Date:** 2025-12-07 22:49:17

## Summary

| Metric | Value |
|--------|-------|
| Tests Found | 17 |
| Tests Started | 17 |
| Tests Succeeded | 15  |
| Tests Failed | 2 X |
| Tests Skipped | 0 O |
| Total Time | 826 ms |

## Failures

### Verify Critical Indices Exist

```
Index idx_invoice_date_time should exist on invoice table ==> expected: <true> but was: <false>
```

### Cache Performance: Compare Miss vs Hit

```
Cache hit should be at least 5x faster, actual: 1.0x ==> expected: <true> but was: <false>
```

## Conclusion

X **SOME TESTS FAILED**

Please review the failures above and address the issues.
