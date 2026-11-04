package com.knoux.kxprivatevpn.platform.usage

/** Newest [maxRecords] sessions, oldest first; used by persistence without Android dependencies. */
fun appendMeasured(
    existing: List<UsageRecord>,
    record: UsageRecord,
    maxRecords: Int = 2_000,
): List<UsageRecord> {
    require(maxRecords > 0) { "Retention must keep at least one record" }
    return (existing + record)
        .sortedBy { it.startedAtMillis }
        .takeLast(maxRecords)
}
