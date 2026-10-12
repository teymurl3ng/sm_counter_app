package com.saaf.milkshop.application.readmodels

import androidx.room3.useReaderConnection
import com.saaf.milkshop.data.CanonicalMoneySql
import com.saaf.milkshop.data.MilkShopDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Random

@RunWith(AndroidJUnit4::class)
class CanonicalMoneySqlTest {
    @Test
    fun decimalBoundaryAndAdjacentValuesMatchIndependentDecimalOracle() = runBlocking<Unit> {
        val db = MilkShopDatabase.createInMemory()
        try {
            val values = mutableListOf(0.0, 1.005, -1.005, 2.125, -2.125,
                0.005, -0.005, 61.25, 1e12, -1e12, 1e-20, 1e17, -1e17)
            val random = Random(71011)
            repeat(1000) {
                val tie = (random.nextInt(200000000) - 100000000) / 100.0 + .005
                values.addAll(listOf(tie, Math.nextDown(tie), Math.nextUp(tie)))
            }
            db.useReaderConnection { connection ->
                for (value in values) {
                    connection.usePrepared("SELECT ${CanonicalMoneySql.ROUND_VALUE} FROM (SELECT ? AS money_raw)") { stmt ->
                        stmt.bindDouble(1, value)
                        assertTrue(stmt.step())
                        val expected = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
                        assertEquals("HALF_UP($value)", expected, stmt.getDouble(0), 0.0)
                    }
                }
            }
        } finally { db.close() }
    }
}
