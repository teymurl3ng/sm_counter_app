package com.saaf.milkshop.data

import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * go 8 leg 4: seed rows must be BYTE-IDENTICAL across fresh installations.
 * CURRENT_TIMESTAMP stamps varied per install, so every fresh enrollment
 * churned its seed rows through the server conflict auditor once (benign but
 * noisy — 16 audits at the 2026-10-03 S24 enrollment). Seed and backfill SQL
 * therefore carry a fixed epoch constant; this test fails if a volatile
 * stamp ever returns.
 */
class SeedDeterminismTest {

    @Test
    fun seedStatements_carryNoVolatileTimestamps() {
        DatabaseSql.seedStatements.forEachIndexed { index, sql ->
            assertFalse(
                "seedStatements[$index] stamps CURRENT_TIMESTAMP — fresh installs would push byte-different rows: $sql",
                sql.contains("CURRENT_TIMESTAMP"),
            )
        }
    }

    @Test
    fun profileBackfillStatements_carryNoVolatileTimestamps() {
        Phase3MigrationSql.backfillStatements.forEachIndexed { index, sql ->
            assertFalse(
                "backfillStatements[$index] stamps CURRENT_TIMESTAMP — fresh installs would push byte-different profiles: $sql",
                sql.contains("CURRENT_TIMESTAMP"),
            )
        }
    }
}
