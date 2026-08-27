package com.saaf.milkshop.data

internal object Phase3SecurityMigrationSql {
    val createStatements: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS app_users (
            user_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            login_name TEXT NOT NULL,
            display_name TEXT NOT NULL,
            role TEXT NOT NULL,
            employee_account_id INTEGER,
            status TEXT NOT NULL DEFAULT 'active',
            credential_setup_required INTEGER NOT NULL DEFAULT 0,
            created_at TEXT NOT NULL,
            created_by_user_id INTEGER,
            updated_at TEXT NOT NULL,
            disabled_at TEXT,
            disabled_by_user_id INTEGER,
            FOREIGN KEY(employee_account_id) REFERENCES employee_profiles(account_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(created_by_user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(disabled_by_user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE UNIQUE INDEX IF NOT EXISTS index_app_users_login_name ON app_users(login_name)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_app_users_employee_account_id ON app_users(employee_account_id)",
        "CREATE INDEX IF NOT EXISTS index_app_users_created_by_user_id ON app_users(created_by_user_id)",
        "CREATE INDEX IF NOT EXISTS index_app_users_disabled_by_user_id ON app_users(disabled_by_user_id)",
        """
        CREATE TABLE IF NOT EXISTS user_credentials (
            user_id INTEGER NOT NULL PRIMARY KEY,
            algorithm TEXT NOT NULL,
            salt BLOB NOT NULL,
            verifier BLOB NOT NULL,
            memory_kib INTEGER NOT NULL,
            iterations INTEGER NOT NULL,
            parallelism INTEGER NOT NULL,
            changed_at TEXT NOT NULL,
            must_change INTEGER NOT NULL DEFAULT 0,
            failed_attempt_count INTEGER NOT NULL DEFAULT 0,
            last_failed_at_epoch_ms INTEGER,
            locked_until_epoch_ms INTEGER,
            FOREIGN KEY(user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS saved_recovery_codes (
            recovery_code_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            user_id INTEGER NOT NULL,
            salt BLOB NOT NULL,
            verifier BLOB NOT NULL,
            created_at TEXT NOT NULL,
            used_at TEXT,
            failed_attempt_count INTEGER NOT NULL DEFAULT 0,
            locked_until_epoch_ms INTEGER,
            FOREIGN KEY(user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_saved_recovery_codes_user_id ON saved_recovery_codes(user_id)",
        """
        CREATE TABLE IF NOT EXISTS security_audit_entries (
            audit_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            occurred_at TEXT NOT NULL,
            action TEXT NOT NULL,
            result TEXT NOT NULL,
            reason_code TEXT,
            actor_user_id INTEGER,
            actor_name_snapshot TEXT,
            actor_role_snapshot TEXT,
            target_user_id INTEGER,
            details TEXT,
            FOREIGN KEY(actor_user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(target_user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_security_audit_entries_occurred_at ON security_audit_entries(occurred_at)",
        "CREATE INDEX IF NOT EXISTS index_security_audit_entries_actor_user_id ON security_audit_entries(actor_user_id)",
        "CREATE INDEX IF NOT EXISTS index_security_audit_entries_target_user_id ON security_audit_entries(target_user_id)",
        """
        CREATE TABLE IF NOT EXISTS event_actor_links (
            event_id INTEGER NOT NULL PRIMARY KEY,
            actor_user_id INTEGER NOT NULL,
            actor_name_snapshot TEXT NOT NULL,
            actor_role_snapshot TEXT NOT NULL,
            attributed_at TEXT NOT NULL,
            FOREIGN KEY(event_id) REFERENCES events(event_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(actor_user_id) REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_event_actor_links_actor_user_id ON event_actor_links(actor_user_id)",
        "ALTER TABLE daily_close_actions ADD COLUMN actor_user_id INTEGER REFERENCES app_users(user_id) ON UPDATE NO ACTION ON DELETE NO ACTION",
        "CREATE INDEX IF NOT EXISTS index_daily_close_actions_actor_user_id ON daily_close_actions(actor_user_id)",
    )

    val triggerNames: List<String> = listOf(
        "validate_app_user_insert",
        "validate_app_user_update",
        "prevent_app_user_delete",
        "prevent_last_owner_update",
        "validate_user_credential",
        "prevent_security_audit_update",
        "prevent_security_audit_delete",
        "validate_verified_daily_close_actor",
    )

    val createTriggers: List<String> = listOf(
        userValidationTrigger("validate_app_user_insert", "INSERT"),
        userValidationTrigger("validate_app_user_update", "UPDATE"),
        """
        CREATE TRIGGER IF NOT EXISTS prevent_app_user_delete
        BEFORE DELETE ON app_users
        BEGIN
            SELECT RAISE(ABORT, 'Application users are disabled, not deleted');
        END
        """.trimIndent(),
        """
        CREATE TRIGGER IF NOT EXISTS prevent_last_owner_update
        BEFORE UPDATE ON app_users
        WHEN OLD.role = 'owner' AND OLD.status = 'active'
         AND (NEW.role <> 'owner' OR NEW.status <> 'active')
         AND (SELECT COUNT(*) FROM app_users WHERE role = 'owner' AND status = 'active') <= 1
        BEGIN
            SELECT RAISE(ABORT, 'The final active owner cannot be disabled or demoted');
        END
        """.trimIndent(),
        """
        CREATE TRIGGER IF NOT EXISTS validate_user_credential
        BEFORE INSERT ON user_credentials
        BEGIN
            SELECT CASE
                WHEN NEW.algorithm <> 'argon2id' THEN RAISE(ABORT, 'Credential algorithm must be Argon2id')
                WHEN NEW.memory_kib < 19456 OR NEW.iterations < 2 OR NEW.parallelism < 1
                    THEN RAISE(ABORT, 'Credential work parameters are below the approved minimum')
                WHEN length(NEW.salt) < 16 OR length(NEW.verifier) < 32
                    THEN RAISE(ABORT, 'Credential salt or verifier is too short')
            END;
        END
        """.trimIndent(),
        "CREATE TRIGGER IF NOT EXISTS prevent_security_audit_update BEFORE UPDATE ON security_audit_entries BEGIN SELECT RAISE(ABORT, 'Security audit is append-only'); END",
        "CREATE TRIGGER IF NOT EXISTS prevent_security_audit_delete BEFORE DELETE ON security_audit_entries BEGIN SELECT RAISE(ABORT, 'Security audit is append-only'); END",
        """
        CREATE TRIGGER IF NOT EXISTS validate_verified_daily_close_actor
        BEFORE INSERT ON daily_close_actions
        WHEN EXISTS (SELECT 1 FROM app_users)
        BEGIN
            SELECT CASE
                WHEN NEW.actor_user_id IS NULL THEN RAISE(ABORT, 'Daily close requires a verified user')
                WHEN NOT EXISTS (
                    SELECT 1 FROM app_users u
                    WHERE u.user_id = NEW.actor_user_id
                      AND u.status = 'active'
                      AND u.display_name = NEW.actor_name
                      AND u.role = NEW.actor_role
                ) THEN RAISE(ABORT, 'Daily close actor does not match an active verified user')
                WHEN NEW.action = 'reopen' AND NOT EXISTS (
                    SELECT 1 FROM app_users u
                    WHERE u.user_id = NEW.actor_user_id AND u.role IN ('owner', 'admin')
                ) THEN RAISE(ABORT, 'Only a verified owner or admin can reopen a date')
            END;
        END
        """.trimIndent(),
    )

    private fun userValidationTrigger(name: String, operation: String): String = """
        CREATE TRIGGER IF NOT EXISTS $name
        BEFORE $operation ON app_users
        BEGIN
            SELECT CASE
                WHEN NEW.login_name <> lower(NEW.login_name)
                     OR length(NEW.login_name) < 3 OR length(NEW.login_name) > 32
                     OR NEW.login_name GLOB '*[^a-z0-9._-]*'
                    THEN RAISE(ABORT, 'Login name must be 3-32 lowercase ASCII characters')
                WHEN trim(NEW.display_name) = '' THEN RAISE(ABORT, 'User display name is required')
                WHEN NEW.role NOT IN ('owner', 'admin', 'manager', 'cashier', 'operator')
                    THEN RAISE(ABORT, 'Application user role is invalid')
                WHEN NEW.status NOT IN ('active', 'disabled')
                    THEN RAISE(ABORT, 'Application user status is invalid')
            END;
        END
    """.trimIndent()
}
