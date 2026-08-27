package com.saaf.milkshop.data

object Phase3DailyCloseMigrationSql {
    val createStatements = listOf(
        """
        CREATE TABLE IF NOT EXISTS daily_close_actions (
            action_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            business_date TEXT NOT NULL,
            action TEXT NOT NULL,
            physical_cash_count_id INTEGER,
            actor_name TEXT NOT NULL,
            actor_role TEXT NOT NULL,
            reason TEXT,
            created_at TEXT NOT NULL,
            supersedes_action_id INTEGER,
            FOREIGN KEY(physical_cash_count_id) REFERENCES physical_cash_counts(count_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(supersedes_action_id) REFERENCES daily_close_actions(action_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_daily_close_actions_business_date ON daily_close_actions(business_date)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_daily_close_actions_physical_cash_count_id ON daily_close_actions(physical_cash_count_id)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_daily_close_actions_supersedes_action_id ON daily_close_actions(supersedes_action_id)",
    )
}
