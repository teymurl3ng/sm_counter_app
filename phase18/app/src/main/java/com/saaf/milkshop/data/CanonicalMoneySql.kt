package com.saaf.milkshop.data

/** Decimal HALF_UP after summing raw ledger facts. Do not pre-round ledger rows.
 * printf's first round-tripping representation preserves both decimal ties and
 * adjacent binary values. The digit cut avoids floating point multiplication.
 * Out-of-range values remain raw so the application's exact paisa conversion rejects them.
 */
internal object CanonicalMoneySql {
    const val ROUND_VALUE = """(
WITH
money_input AS (SELECT COALESCE(money_raw, 0.0) AS value),
money_text AS (
 SELECT value,
 CASE WHEN CAST(printf('%!.15g', value) AS REAL) = value THEN printf('%!.15g', value)
      WHEN CAST(printf('%!.16g', value) AS REAL) = value THEN printf('%!.16g', value)
      ELSE printf('%!.17g', value) END AS decimal_text
 FROM money_input
),
money_unsigned AS (
 SELECT value, CASE WHEN value < 0 THEN -1 ELSE 1 END AS sign,
 CASE WHEN substr(decimal_text, 1, 1) = '-' THEN substr(decimal_text, 2)
      ELSE decimal_text END AS text FROM money_text
),
money_parts AS (
 SELECT value, sign,
 CASE WHEN instr(text, 'e') > 0 THEN substr(text, 1, instr(text, 'e') - 1) ELSE text END AS mantissa,
 CASE WHEN instr(text, 'e') > 0 THEN CAST(substr(text, instr(text, 'e') + 1) AS INTEGER) ELSE 0 END AS exponent
 FROM money_unsigned
),
money_digits AS (
 SELECT value, sign, replace(mantissa, '.', '') AS digits,
 CASE WHEN instr(mantissa, '.') > 0 THEN instr(mantissa, '.') - 1 ELSE length(mantissa) END + exponent + 2 AS cut
 FROM money_parts
),
money_split AS (
 SELECT value, sign,
 CASE WHEN cut <= 0 THEN '0'
      WHEN cut > 19 THEN 'overflow'
      ELSE substr(digits, 1, cut) || substr('0000000000000000000', 1, max(0, cut - length(digits))) END AS kept,
 CASE WHEN cut < 0 THEN 0
      WHEN substr(digits, cut + 1, 1) >= '5' THEN 1 ELSE 0 END AS carry
 FROM money_digits
),
money_integer AS (
 SELECT value, sign, kept, carry, ltrim(kept, '0') AS significant FROM money_split
)
SELECT CASE
 WHEN kept = 'overflow' OR length(significant) > 19
      OR (length(significant) = 19 AND significant >= '9223372036854775807') THEN value
 ELSE (CAST(kept AS INTEGER) + carry) * sign / 100.0 END
FROM money_integer
)"""

    // LIMIT/OFFSET prevents old SQLite from flattening a correlated aggregate
    // into the nested rounding CTE (misuse of SUM). It does not limit rows.
    const val ACCOUNT_BALANCES = """
        WITH raw_balances AS (
            SELECT a.account_id, a.name, a.type, a.category, SUM(fl.amount) AS money_raw
            FROM accounts a LEFT JOIN financial_log fl ON fl.account_id = a.account_id
            GROUP BY a.account_id, a.name, a.type, a.category
            LIMIT -1 OFFSET 0
        )
        SELECT account_id, name, type, category, """ + ROUND_VALUE + """ AS balance FROM raw_balances
    """

    const val BALANCE_AT = """
        WITH raw_sum AS (
            SELECT SUM(fl.amount) AS money_raw
            FROM financial_log fl INNER JOIN events e ON e.event_id = fl.event_id
            WHERE fl.account_id = :accountId AND e.created_at <= :createdAtInclusive
            LIMIT -1 OFFSET 0
        )
        SELECT """ + ROUND_VALUE + """ AS balance FROM raw_sum
    """

    const val CLOSE_BALANCE_AT = """(
        WITH raw_sum AS (
            SELECT SUM(fl.amount) AS money_raw
            FROM financial_log fl INNER JOIN events e ON e.event_id = fl.event_id
            WHERE fl.account_id = (SELECT account_id FROM physical_cash_counts WHERE count_id = NEW.physical_cash_count_id)
            AND e.created_at <= (SELECT counted_at FROM physical_cash_counts WHERE count_id = NEW.physical_cash_count_id)
            LIMIT -1 OFFSET 0
        )
        SELECT """ + ROUND_VALUE + """ FROM raw_sum
    )"""
}
