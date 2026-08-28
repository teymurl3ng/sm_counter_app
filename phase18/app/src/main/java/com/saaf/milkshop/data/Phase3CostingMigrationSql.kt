package com.saaf.milkshop.data

object Phase3CostingMigrationSql {
    val createStatements = listOf(
        """
        CREATE TABLE IF NOT EXISTS costing_policy_versions (
            policy_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            calendar_month TEXT NOT NULL,
            normal_capacity_kg TEXT NOT NULL,
            review_reason TEXT NOT NULL,
            notes TEXT,
            approved_at TEXT NOT NULL,
            approved_by_user_id INTEGER NOT NULL,
            supersedes_policy_id INTEGER,
            FOREIGN KEY(approved_by_user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(supersedes_policy_id) REFERENCES costing_policy_versions(policy_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_costing_policy_versions_calendar_month ON costing_policy_versions(calendar_month)",
        "CREATE INDEX IF NOT EXISTS index_costing_policy_versions_approved_by_user_id ON costing_policy_versions(approved_by_user_id)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_costing_policy_versions_supersedes_policy_id ON costing_policy_versions(supersedes_policy_id)",
        """
        CREATE TABLE IF NOT EXISTS costing_policy_components (
            policy_id INTEGER NOT NULL,
            component_code TEXT NOT NULL,
            source_amount_paisa INTEGER NOT NULL,
            basis_quantity_kg TEXT NOT NULL,
            PRIMARY KEY(policy_id, component_code),
            FOREIGN KEY(policy_id) REFERENCES costing_policy_versions(policy_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS inventory_cost_basis_entries (
            basis_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            opening_event_id INTEGER NOT NULL,
            product_id INTEGER NOT NULL,
            quantity TEXT NOT NULL,
            total_cost_paisa INTEGER NOT NULL,
            basis_kind TEXT NOT NULL,
            evidence_notes TEXT,
            created_at TEXT NOT NULL,
            created_by_user_id INTEGER NOT NULL,
            supersedes_basis_id INTEGER,
            FOREIGN KEY(opening_event_id) REFERENCES events(event_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(product_id) REFERENCES products(product_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(created_by_user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(supersedes_basis_id) REFERENCES inventory_cost_basis_entries(basis_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_inventory_cost_basis_entries_opening_event_id_product_id ON inventory_cost_basis_entries(opening_event_id, product_id)",
        "CREATE INDEX IF NOT EXISTS index_inventory_cost_basis_entries_product_id ON inventory_cost_basis_entries(product_id)",
        "CREATE INDEX IF NOT EXISTS index_inventory_cost_basis_entries_created_by_user_id ON inventory_cost_basis_entries(created_by_user_id)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_inventory_cost_basis_entries_supersedes_basis_id ON inventory_cost_basis_entries(supersedes_basis_id)",
        """
        CREATE TABLE IF NOT EXISTS yield_policy_versions (
            yield_policy_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            sample_start_date TEXT NOT NULL,
            sample_end_date TEXT NOT NULL,
            accepted_batch_count INTEGER NOT NULL,
            median_yield_ratio TEXT NOT NULL,
            mad_yield_ratio TEXT NOT NULL,
            lower_alert_limit TEXT NOT NULL,
            upper_alert_limit TEXT NOT NULL,
            effective_from TEXT NOT NULL,
            approved_at TEXT NOT NULL,
            approved_by_user_id INTEGER NOT NULL,
            supersedes_yield_policy_id INTEGER,
            FOREIGN KEY(approved_by_user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(supersedes_yield_policy_id) REFERENCES yield_policy_versions(yield_policy_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_yield_policy_versions_effective_from ON yield_policy_versions(effective_from)",
        "CREATE INDEX IF NOT EXISTS index_yield_policy_versions_approved_by_user_id ON yield_policy_versions(approved_by_user_id)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_yield_policy_versions_supersedes_yield_policy_id ON yield_policy_versions(supersedes_yield_policy_id)",
        """
        CREATE TABLE IF NOT EXISTS production_loss_assessments (
            assessment_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            production_event_id INTEGER NOT NULL,
            classification TEXT NOT NULL,
            abnormal_milk_liters TEXT NOT NULL,
            reason_code TEXT NOT NULL,
            notes TEXT,
            assessed_at TEXT NOT NULL,
            assessed_by_user_id INTEGER NOT NULL,
            supersedes_assessment_id INTEGER,
            FOREIGN KEY(production_event_id) REFERENCES events(event_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(assessed_by_user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(supersedes_assessment_id) REFERENCES production_loss_assessments(assessment_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_production_loss_assessments_production_event_id ON production_loss_assessments(production_event_id)",
        "CREATE INDEX IF NOT EXISTS index_production_loss_assessments_assessed_by_user_id ON production_loss_assessments(assessed_by_user_id)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_production_loss_assessments_supersedes_assessment_id ON production_loss_assessments(supersedes_assessment_id)",
        """
        CREATE TABLE IF NOT EXISTS inventory_cost_movements (
            movement_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            source_event_id INTEGER NOT NULL,
            product_id INTEGER NOT NULL,
            movement_role TEXT NOT NULL,
            movement_kind TEXT NOT NULL,
            sequence_number INTEGER NOT NULL,
            occurred_at TEXT NOT NULL,
            created_at TEXT NOT NULL,
            quantity_in TEXT,
            quantity_out TEXT,
            cost_in_paisa INTEGER,
            cost_out_paisa INTEGER,
            running_quantity TEXT NOT NULL,
            running_cost_paisa INTEGER,
            coverage_code TEXT NOT NULL,
            applied_policy_id INTEGER,
            applied_yield_policy_id INTEGER,
            applied_assessment_id INTEGER,
            applied_basis_id INTEGER,
            FOREIGN KEY(source_event_id) REFERENCES events(event_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(product_id) REFERENCES products(product_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE UNIQUE INDEX IF NOT EXISTS index_inventory_cost_movements_source_event_id_product_id_movement_role ON inventory_cost_movements(source_event_id, product_id, movement_role)",
        "CREATE INDEX IF NOT EXISTS index_inventory_cost_movements_product_id_sequence_number ON inventory_cost_movements(product_id, sequence_number)",
        "CREATE INDEX IF NOT EXISTS index_inventory_cost_movements_occurred_at ON inventory_cost_movements(occurred_at)",
        """
        CREATE TABLE IF NOT EXISTS cost_projection_state (
            state_id INTEGER NOT NULL PRIMARY KEY,
            engine_version INTEGER NOT NULL,
            source_event_count INTEGER NOT NULL,
            source_max_event_id INTEGER NOT NULL,
            source_max_policy_id INTEGER NOT NULL,
            source_max_basis_id INTEGER NOT NULL,
            source_max_yield_policy_id INTEGER NOT NULL,
            source_max_assessment_id INTEGER NOT NULL,
            rebuilt_at TEXT NOT NULL,
            coverage_code TEXT NOT NULL
        )
        """.trimIndent(),
    )

    val immutableTriggerNames = listOf(
        "costing_policy_versions_no_update",
        "costing_policy_versions_no_delete",
        "costing_policy_components_no_update",
        "costing_policy_components_no_delete",
        "inventory_cost_basis_entries_no_update",
        "inventory_cost_basis_entries_no_delete",
        "yield_policy_versions_no_update",
        "yield_policy_versions_no_delete",
        "production_loss_assessments_no_update",
        "production_loss_assessments_no_delete",
    )

    val immutableTriggers = listOf(
        immutableUpdate("costing_policy_versions"), immutableDelete("costing_policy_versions"),
        immutableUpdate("costing_policy_components"), immutableDelete("costing_policy_components"),
        immutableUpdate("inventory_cost_basis_entries"), immutableDelete("inventory_cost_basis_entries"),
        immutableUpdate("yield_policy_versions"), immutableDelete("yield_policy_versions"),
        immutableUpdate("production_loss_assessments"), immutableDelete("production_loss_assessments"),
    )

    private fun immutableUpdate(table: String): String = """
        CREATE TRIGGER IF NOT EXISTS ${table}_no_update
        BEFORE UPDATE ON $table
        BEGIN
            SELECT RAISE(ABORT, '$table is append-only');
        END
    """.trimIndent()

    private fun immutableDelete(table: String): String = """
        CREATE TRIGGER IF NOT EXISTS ${table}_no_delete
        BEFORE DELETE ON $table
        BEGIN
            SELECT RAISE(ABORT, '$table is append-only');
        END
    """.trimIndent()
}
