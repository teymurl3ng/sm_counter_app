package com.saaf.milkshop.data

import com.saaf.milkshop.sync.LegacyUid

/**
 * SQLite-only integrity objects that Room's entity annotations do not express:
 * CHECK-equivalent validation triggers, business-direction triggers, and the
 * per-connection foreign-key pragma.
 *
 * The baseline statements reproduce the frozen SQL blueprint's integrity
 * behavior. Explicitly approved additive schema versions extend this list with
 * their corresponding native integrity rules.
 */
internal object DatabaseSql {
    val triggerNames: List<String> = listOf(
        "validate_product_values",
        "validate_account_values",
        "validate_event_values",
        "validate_inventory_values",
        "validate_financial_values",
        "validate_setting_values",
        "validate_setting_updates",
        "validate_store_profile_insert",
        "validate_store_profile_update",
        "validate_opening_batch",
        "validate_opening_inventory_entry",
        "validate_opening_account_entry",
        "validate_daily_close_action",
        "prevent_closed_event",
        "prevent_closed_cash_count",
        "prevent_closed_attendance_insert",
        "prevent_closed_attendance_update",
        "prevent_closed_delivery_insert",
        "prevent_closed_delivery_update",
        "prevent_closed_delivery_status",
        "validate_correction_event_type",
        "validate_purchase_inventory",
        "validate_production_inventory",
        "validate_sale_inventory",
        "validate_spoilage_inventory",
        "validate_opening_inventory",
        "reject_unexpected_inventory_event",
        "prevent_negative_stock",
        "validate_financial_movement",
    )

    val createTriggers: List<String> = listOf(
        // ------------------------------------------------------------------
        // TABLE-CHECK EQUIVALENTS
        // ------------------------------------------------------------------
        """
        CREATE TRIGGER IF NOT EXISTS validate_product_values
        BEFORE INSERT ON products
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.unit NOT IN ('Liters', 'Kg')
                    THEN RAISE(ABORT, 'Product unit must be Liters or Kg')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_account_values
        BEFORE INSERT ON accounts
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.type NOT IN ('Asset', 'Liability', 'Expense')
                    THEN RAISE(ABORT, 'Invalid account type')
                WHEN NEW.category NOT IN ('Cash/Bank', 'Vendor', 'Customer', 'Operating Expense', 'Salary')
                    THEN RAISE(ABORT, 'Invalid account category')
                WHEN NOT (
                    (NEW.category = 'Cash/Bank' AND NEW.type = 'Asset') OR
                    (NEW.category = 'Vendor' AND NEW.type = 'Liability') OR
                    (NEW.category = 'Customer' AND NEW.type = 'Asset') OR
                    (NEW.category = 'Operating Expense' AND NEW.type = 'Expense') OR
                    (NEW.category = 'Salary' AND NEW.type = 'Expense')
                )
                    THEN RAISE(ABORT, 'Account type/category combination is invalid')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_event_values
        BEFORE INSERT ON events
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.event_type NOT IN (
                    'PURCHASE_RECEIPT', 'VENDOR_PAYMENT', 'PRODUCTION', 'SALE',
                    'CREDIT_COLLECTION', 'EXPENSE', 'SALARY_PAYMENT', 'SPOILAGE',
                    'OPENING_STATE'
                )
                    THEN RAISE(ABORT, 'Invalid event type')
                WHEN NEW.channel IS NOT NULL AND NEW.channel NOT IN (
                    'retail', 'wholesale', 'credit-retail', 'credit-wholesale'
                )
                    THEN RAISE(ABORT, 'Invalid sales channel')
                WHEN NEW.raw_liters IS NOT NULL AND NEW.raw_liters <= 0
                    THEN RAISE(ABORT, 'Raw liters must be greater than zero')
                WHEN NEW.fat_percentage IS NOT NULL AND NEW.fat_percentage < 0
                    THEN RAISE(ABORT, 'Fat percentage cannot be negative')
                WHEN NEW.lr_reading IS NOT NULL AND NEW.lr_reading <= 0
                    THEN RAISE(ABORT, 'LR reading must be greater than zero')
                WHEN NEW.snf_percentage IS NOT NULL AND NEW.snf_percentage < 0
                    THEN RAISE(ABORT, 'SNF percentage cannot be negative')
                WHEN NEW.ts_percentage IS NOT NULL AND NEW.ts_percentage < 0
                    THEN RAISE(ABORT, 'TS percentage cannot be negative')
                WHEN NEW.adjusted_liters IS NOT NULL AND NEW.adjusted_liters <= 0
                    THEN RAISE(ABORT, 'Adjusted liters must be greater than zero')
                WHEN NEW.amount_owed IS NOT NULL AND NEW.amount_owed < 0
                    THEN RAISE(ABORT, 'Amount owed cannot be negative')
                WHEN NEW.amount IS NOT NULL AND NEW.amount <= 0
                    THEN RAISE(ABORT, 'Amount must be greater than zero')
                WHEN NEW.milk_liters_used IS NOT NULL AND NEW.milk_liters_used <= 0
                    THEN RAISE(ABORT, 'Milk liters used must be greater than zero')
                WHEN NEW.yogurt_kg_produced IS NOT NULL AND NEW.yogurt_kg_produced <= 0
                    THEN RAISE(ABORT, 'Yogurt kg produced must be greater than zero')
                WHEN NEW.milk_rate_used IS NOT NULL AND NEW.milk_rate_used <= 0
                    THEN RAISE(ABORT, 'Milk rate used must be greater than zero')
                WHEN NEW.delivery_fee_used IS NOT NULL AND NEW.delivery_fee_used <= 0
                    THEN RAISE(ABORT, 'Delivery fee used must be greater than zero')
                WHEN NEW.ts_standard_used IS NOT NULL AND NEW.ts_standard_used <= 0
                    THEN RAISE(ABORT, 'TS standard used must be greater than zero')
                WHEN NEW.sale_quantity IS NOT NULL AND NEW.sale_quantity <= 0
                    THEN RAISE(ABORT, 'Sale quantity must be greater than zero')
                WHEN NEW.sale_value IS NOT NULL AND NEW.sale_value <= 0
                    THEN RAISE(ABORT, 'Sale value must be greater than zero')
                WHEN NEW.unit_rate IS NOT NULL AND NEW.unit_rate <= 0
                    THEN RAISE(ABORT, 'Unit rate must be greater than zero')
                WHEN NEW.surcharge_applied < 0
                    THEN RAISE(ABORT, 'Surcharge cannot be negative')
                WHEN NEW.event_type = 'PURCHASE_RECEIPT'
                     AND NOT (
                         NEW.raw_liters IS NOT NULL AND
                         NEW.fat_percentage IS NOT NULL AND
                         NEW.lr_reading IS NOT NULL AND
                         NEW.snf_percentage IS NOT NULL AND
                         NEW.ts_percentage IS NOT NULL AND
                         NEW.adjusted_liters IS NOT NULL AND
                         NEW.amount_owed IS NOT NULL AND
                         NEW.milk_rate_used IS NOT NULL AND
                         NEW.delivery_fee_used IS NOT NULL AND
                         NEW.ts_standard_used IS NOT NULL
                     )
                    THEN RAISE(ABORT, 'PURCHASE_RECEIPT is missing required fields')
                WHEN NEW.event_type = 'SALE'
                     AND NOT (
                         NEW.unit_rate IS NOT NULL AND
                         NEW.sale_quantity IS NOT NULL AND
                         NEW.sale_value IS NOT NULL
                     )
                    THEN RAISE(ABORT, 'SALE is missing required fields')
                WHEN NEW.event_type = 'PRODUCTION'
                     AND NOT (
                         NEW.milk_liters_used IS NOT NULL AND
                         NEW.yogurt_kg_produced IS NOT NULL
                     )
                    THEN RAISE(ABORT, 'PRODUCTION is missing required fields')
                WHEN NEW.event_type IN ('VENDOR_PAYMENT', 'CREDIT_COLLECTION', 'EXPENSE', 'SALARY_PAYMENT')
                     AND NEW.amount IS NULL
                    THEN RAISE(ABORT, 'Money-only event is missing amount')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_inventory_values
        BEFORE INSERT ON inventory_log
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.quantity = 0
                    THEN RAISE(ABORT, 'Inventory quantity cannot be zero')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_financial_values
        BEFORE INSERT ON financial_log
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.amount = 0
                    THEN RAISE(ABORT, 'Financial amount cannot be zero')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_setting_values
        BEFORE INSERT ON settings
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.setting_value <= 0
                    THEN RAISE(ABORT, 'Setting value must be greater than zero')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_setting_updates
        BEFORE UPDATE OF setting_value ON settings
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.setting_value <= 0
                    THEN RAISE(ABORT, 'Setting value must be greater than zero')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_store_profile_insert
        BEFORE INSERT ON store_profile
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.store_profile_id <> 1
                    THEN RAISE(ABORT, 'Store profile must use the singleton ID')
                WHEN trim(NEW.store_name) = ''
                    THEN RAISE(ABORT, 'Store name cannot be empty')
                WHEN NEW.currency_code <> 'PKR'
                    THEN RAISE(ABORT, 'Store currency must be PKR')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_store_profile_update
        BEFORE UPDATE ON store_profile
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.store_profile_id <> 1
                    THEN RAISE(ABORT, 'Store profile must use the singleton ID')
                WHEN trim(NEW.store_name) = ''
                    THEN RAISE(ABORT, 'Store name cannot be empty')
                WHEN NEW.currency_code <> 'PKR'
                    THEN RAISE(ABORT, 'Store currency must be PKR')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_opening_batch
        BEFORE INSERT ON opening_batches
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) <> 'OPENING_STATE'
                    THEN RAISE(ABORT, 'Opening batch must reference an OPENING_STATE event')
                WHEN (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NULL
                     AND EXISTS (
                         SELECT 1
                         FROM opening_batches prior
                         INNER JOIN events prior_event ON prior_event.event_id = prior.event_id
                         WHERE prior_event.corrects_event_id IS NULL
                     )
                    THEN RAISE(ABORT, 'Only one initial opening state is allowed')
                WHEN (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NOT NULL
                     AND NOT EXISTS (
                         SELECT 1 FROM opening_batches prior
                         WHERE prior.event_id = (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id)
                     )
                    THEN RAISE(ABORT, 'Opening correction must reference an opening batch')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_opening_inventory_entry
        BEFORE INSERT ON opening_inventory_entries
        FOR EACH ROW
        WHEN NEW.quantity <= 0
        BEGIN
            SELECT RAISE(ABORT, 'Restated opening quantity must be greater than zero');
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_opening_account_entry
        BEFORE INSERT ON opening_account_entries
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.balance <= 0
                    THEN RAISE(ABORT, 'Restated opening balance must be greater than zero')
                WHEN NOT EXISTS (
                    SELECT 1 FROM accounts
                    WHERE account_id = NEW.account_id
                      AND category IN ('Cash/Bank', 'Customer', 'Vendor')
                )
                    THEN RAISE(ABORT, 'Opening balance account category is invalid')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_daily_close_action
        BEFORE INSERT ON daily_close_actions
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN NEW.action NOT IN ('close', 'reopen')
                    THEN RAISE(ABORT, 'Daily close action is invalid')
                WHEN trim(NEW.actor_name) = ''
                    THEN RAISE(ABORT, 'Daily close actor name is required')
                WHEN NEW.actor_role NOT IN ('owner', 'admin', 'manager', 'cashier', 'operator')
                    THEN RAISE(ABORT, 'Daily close actor role is invalid')
                WHEN NEW.action = 'reopen' AND NEW.actor_role NOT IN ('owner', 'admin')
                    THEN RAISE(ABORT, 'Only an owner or admin can reopen a date')
                WHEN NEW.action = 'reopen' AND trim(COALESCE(NEW.reason, '')) = ''
                    THEN RAISE(ABORT, 'Reopening reason is required')
                WHEN NEW.action = 'close' AND NEW.physical_cash_count_id IS NULL
                    THEN RAISE(ABORT, 'Daily close requires a physical cash count')
                WHEN NEW.action = 'reopen' AND NEW.physical_cash_count_id IS NOT NULL
                    THEN RAISE(ABORT, 'Reopen action cannot carry a cash count')
                WHEN NEW.action = 'close' AND NOT EXISTS (
                    SELECT 1
                    FROM physical_cash_counts pcc
                    INNER JOIN accounts a ON a.account_id = pcc.account_id
                    WHERE pcc.count_id = NEW.physical_cash_count_id
                      AND pcc.business_date = NEW.business_date
                      AND a.name = 'Cash'
                      AND a.category = 'Cash/Bank'
                )
                    THEN RAISE(ABORT, 'Daily close cash count is invalid')
                WHEN NEW.action = 'close' AND EXISTS (
                    SELECT 1
                    FROM physical_cash_counts selected
                    INNER JOIN physical_cash_counts later
                      ON later.account_id = selected.account_id
                     AND later.business_date = selected.business_date
                    WHERE selected.count_id = NEW.physical_cash_count_id
                      AND (later.counted_at > selected.counted_at OR
                           (later.counted_at = selected.counted_at AND later.count_id > selected.count_id))
                )
                    THEN RAISE(ABORT, 'Daily close must use the latest cash count')
                WHEN NEW.supersedes_action_id IS NULL
                     AND NEW.action <> 'close'
                    THEN RAISE(ABORT, 'The first daily close action must close the date')
                WHEN NEW.supersedes_action_id IS NULL
                     AND EXISTS (SELECT 1 FROM daily_close_actions prior WHERE prior.business_date = NEW.business_date)
                    THEN RAISE(ABORT, 'Daily close history must append to the current action')
                WHEN NEW.supersedes_action_id IS NOT NULL
                     AND NOT EXISTS (
                         SELECT 1 FROM daily_close_actions prior
                         WHERE prior.action_id = NEW.supersedes_action_id
                           AND prior.business_date = NEW.business_date
                     )
                    THEN RAISE(ABORT, 'Daily close action must supersede the same business date')
                WHEN NEW.supersedes_action_id IS NOT NULL
                     AND (SELECT action FROM daily_close_actions WHERE action_id = NEW.supersedes_action_id) = NEW.action
                    THEN RAISE(ABORT, 'Daily close and reopen actions must alternate')
                WHEN NEW.action = 'close'
                     AND NEW.supersedes_action_id IS NOT NULL
                     AND (
                         SELECT counted_at FROM physical_cash_counts
                         WHERE count_id = NEW.physical_cash_count_id
                     ) <= (
                         SELECT created_at FROM daily_close_actions
                         WHERE action_id = NEW.supersedes_action_id
                     )
                    THEN RAISE(ABORT, 'Reclose requires a new cash count after reopening')
                WHEN NEW.action = 'close'
                     AND ABS(
                         (SELECT counted_amount FROM physical_cash_counts WHERE count_id = NEW.physical_cash_count_id) -
                         COALESCE((
                             SELECT ROUND(SUM(fl.amount), 2)
                             FROM financial_log fl
                             INNER JOIN events e ON e.event_id = fl.event_id
                             WHERE fl.account_id = (
                                 SELECT account_id FROM physical_cash_counts WHERE count_id = NEW.physical_cash_count_id
                             )
                               AND e.created_at <= (
                                   SELECT counted_at FROM physical_cash_counts WHERE count_id = NEW.physical_cash_count_id
                               )
                         ), 0)
                     ) >= 0.005
                     AND trim(COALESCE(NEW.reason, '')) = ''
                    THEN RAISE(ABORT, 'Non-zero cash variance requires an explanation')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS prevent_closed_event
        BEFORE INSERT ON events
        FOR EACH ROW
        WHEN NOT EXISTS (
            SELECT 1 FROM daily_close_actions exact_action
            WHERE exact_action.business_date = NEW.occurred_at
              AND exact_action.action = 'reopen'
              AND NOT EXISTS (
                  SELECT 1 FROM daily_close_actions child
                  WHERE child.supersedes_action_id = exact_action.action_id
              )
        ) AND EXISTS (
            SELECT 1 FROM daily_close_actions lock_action
            WHERE lock_action.business_date >= NEW.occurred_at
              AND lock_action.action = 'close'
              AND NOT EXISTS (
                  SELECT 1 FROM daily_close_actions child
                  WHERE child.supersedes_action_id = lock_action.action_id
              )
        )
        BEGIN
            SELECT RAISE(ABORT, 'Business date is closed; reopen it before posting');
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS prevent_closed_cash_count
        BEFORE INSERT ON physical_cash_counts
        FOR EACH ROW
        WHEN NOT EXISTS (
            SELECT 1 FROM daily_close_actions exact_action
            WHERE exact_action.business_date = NEW.business_date
              AND exact_action.action = 'reopen'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = exact_action.action_id)
        ) AND EXISTS (
            SELECT 1 FROM daily_close_actions lock_action
            WHERE lock_action.business_date >= NEW.business_date
              AND lock_action.action = 'close'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = lock_action.action_id)
        )
        BEGIN
            SELECT RAISE(ABORT, 'Business date is closed; reopen it before counting cash');
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS prevent_closed_attendance_insert
        BEFORE INSERT ON attendance_records
        FOR EACH ROW
        WHEN NOT EXISTS (
            SELECT 1 FROM daily_close_actions exact_action
            WHERE exact_action.business_date = NEW.business_date AND exact_action.action = 'reopen'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = exact_action.action_id)
        ) AND EXISTS (
            SELECT 1 FROM daily_close_actions lock_action
            WHERE lock_action.business_date >= NEW.business_date AND lock_action.action = 'close'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = lock_action.action_id)
        )
        BEGIN
            SELECT RAISE(ABORT, 'Business date is closed; reopen it before changing attendance');
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS prevent_closed_attendance_update
        BEFORE UPDATE ON attendance_records
        FOR EACH ROW
        WHEN NOT EXISTS (
            SELECT 1 FROM daily_close_actions exact_action
            WHERE exact_action.business_date = OLD.business_date AND exact_action.action = 'reopen'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = exact_action.action_id)
        ) AND EXISTS (
            SELECT 1 FROM daily_close_actions lock_action
            WHERE lock_action.business_date >= OLD.business_date AND lock_action.action = 'close'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = lock_action.action_id)
        )
        BEGIN
            SELECT RAISE(ABORT, 'Business date is closed; reopen it before changing attendance');
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS prevent_closed_delivery_insert
        BEFORE INSERT ON deliveries
        FOR EACH ROW
        WHEN NOT EXISTS (
            SELECT 1 FROM daily_close_actions exact_action
            WHERE exact_action.business_date = NEW.business_date AND exact_action.action = 'reopen'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = exact_action.action_id)
        ) AND EXISTS (
            SELECT 1 FROM daily_close_actions lock_action
            WHERE lock_action.business_date >= NEW.business_date AND lock_action.action = 'close'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = lock_action.action_id)
        )
        BEGIN
            SELECT RAISE(ABORT, 'Business date is closed; reopen it before changing deliveries');
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS prevent_closed_delivery_update
        BEFORE UPDATE ON deliveries
        FOR EACH ROW
        WHEN NOT EXISTS (
            SELECT 1 FROM daily_close_actions exact_action
            WHERE exact_action.business_date = OLD.business_date AND exact_action.action = 'reopen'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = exact_action.action_id)
        ) AND EXISTS (
            SELECT 1 FROM daily_close_actions lock_action
            WHERE lock_action.business_date >= OLD.business_date AND lock_action.action = 'close'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = lock_action.action_id)
        )
        BEGIN
            SELECT RAISE(ABORT, 'Business date is closed; reopen it before changing deliveries');
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS prevent_closed_delivery_status
        BEFORE INSERT ON delivery_status_history
        FOR EACH ROW
        WHEN NOT EXISTS (
            SELECT 1 FROM daily_close_actions exact_action
            WHERE exact_action.business_date = (SELECT business_date FROM deliveries WHERE delivery_id = NEW.delivery_id)
              AND exact_action.action = 'reopen'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = exact_action.action_id)
        ) AND EXISTS (
            SELECT 1 FROM daily_close_actions lock_action
            WHERE lock_action.business_date >= (SELECT business_date FROM deliveries WHERE delivery_id = NEW.delivery_id)
              AND lock_action.action = 'close'
              AND NOT EXISTS (SELECT 1 FROM daily_close_actions child WHERE child.supersedes_action_id = lock_action.action_id)
        )
        BEGIN
            SELECT RAISE(ABORT, 'Business date is closed; reopen it before changing deliveries');
        END;
        """.trimIndent(),

        // ------------------------------------------------------------------
        // BUSINESS-DIRECTION / CORRECTION INTEGRITY
        // ------------------------------------------------------------------
        """
        CREATE TRIGGER IF NOT EXISTS validate_correction_event_type
        BEFORE INSERT ON events
        FOR EACH ROW
        WHEN NEW.corrects_event_id IS NOT NULL
        BEGIN
            SELECT CASE
                WHEN (SELECT event_type FROM events WHERE event_id = NEW.corrects_event_id) <> NEW.event_type
                    THEN RAISE(ABORT, 'A correction event must share event_type with the event it corrects')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_purchase_inventory
        BEFORE INSERT ON inventory_log
        FOR EACH ROW
        WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'PURCHASE_RECEIPT'
        BEGIN
            SELECT CASE
                WHEN (SELECT name FROM products WHERE product_id = NEW.product_id) <> 'Milk'
                    THEN RAISE(ABORT, 'PURCHASE_RECEIPT inventory must be Milk')
                WHEN (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NULL
                     AND NEW.quantity <= 0
                    THEN RAISE(ABORT, 'PURCHASE_RECEIPT inventory quantity must be positive')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_production_inventory
        BEFORE INSERT ON inventory_log
        FOR EACH ROW
        WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'PRODUCTION'
        BEGIN
            SELECT CASE
                WHEN (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NULL
                     AND (SELECT name FROM products WHERE product_id = NEW.product_id) = 'Milk'
                     AND NEW.quantity >= 0
                    THEN RAISE(ABORT, 'PRODUCTION must consume Milk')
                WHEN (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NULL
                     AND (SELECT name FROM products WHERE product_id = NEW.product_id) = 'Yogurt'
                     AND NEW.quantity <= 0
                    THEN RAISE(ABORT, 'PRODUCTION must produce Yogurt')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_sale_inventory
        BEFORE INSERT ON inventory_log
        FOR EACH ROW
        WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'SALE'
        BEGIN
            SELECT CASE
                WHEN (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NULL
                     AND NEW.quantity >= 0
                    THEN RAISE(ABORT, 'SALE inventory quantity must be negative')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_spoilage_inventory
        BEFORE INSERT ON inventory_log
        FOR EACH ROW
        WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'SPOILAGE'
        BEGIN
            SELECT CASE
                WHEN (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NULL
                     AND NEW.quantity >= 0
                    THEN RAISE(ABORT, 'SPOILAGE inventory quantity must be negative')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_opening_inventory
        BEFORE INSERT ON inventory_log
        FOR EACH ROW
        WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'OPENING_STATE'
        BEGIN
            SELECT CASE
                WHEN (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NULL
                     AND NEW.quantity <= 0
                    THEN RAISE(ABORT, 'Initial opening inventory must be positive')
            END;
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS reject_unexpected_inventory_event
        BEFORE INSERT ON inventory_log
        FOR EACH ROW
        WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id)
             NOT IN ('PURCHASE_RECEIPT', 'PRODUCTION', 'SALE', 'SPOILAGE', 'OPENING_STATE')
        BEGIN
            SELECT RAISE(ABORT, 'This event type cannot create inventory movement');
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS prevent_negative_stock
        BEFORE INSERT ON inventory_log
        FOR EACH ROW
        WHEN (
            (SELECT COALESCE(SUM(quantity), 0)
             FROM inventory_log
             WHERE product_id = NEW.product_id)
            + NEW.quantity < 0
        )
        BEGIN
            SELECT RAISE(ABORT, 'Insufficient stock for this product');
        END;
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS validate_financial_movement
        BEFORE INSERT ON financial_log
        FOR EACH ROW
        BEGIN
            SELECT CASE
                WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'PURCHASE_RECEIPT'
                     AND NOT EXISTS (
                         SELECT 1 FROM accounts
                         WHERE account_id = NEW.account_id
                           AND category = 'Vendor'
                     )
                    THEN RAISE(ABORT, 'PURCHASE_RECEIPT must increase a Vendor payable')

                WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'PURCHASE_RECEIPT'
                     AND (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NULL
                     AND NEW.amount <= 0
                    THEN RAISE(ABORT, 'PURCHASE_RECEIPT payable must increase')

                WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'VENDOR_PAYMENT'
                     AND NOT EXISTS (
                         SELECT 1 FROM accounts
                         WHERE account_id = NEW.account_id
                           AND category IN ('Vendor', 'Cash/Bank')
                     )
                    THEN RAISE(ABORT, 'VENDOR_PAYMENT must affect a Vendor or Cash/Bank account')

                WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'SALE'
                     AND NOT EXISTS (
                         SELECT 1 FROM accounts
                         WHERE account_id = NEW.account_id
                           AND category IN ('Cash/Bank', 'Customer')
                     )
                    THEN RAISE(ABORT, 'SALE must affect Cash/Bank or Customer')

                WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'CREDIT_COLLECTION'
                     AND NOT EXISTS (
                         SELECT 1 FROM accounts
                         WHERE account_id = NEW.account_id
                           AND category IN ('Customer', 'Cash/Bank')
                     )
                    THEN RAISE(ABORT, 'CREDIT_COLLECTION must affect Customer or Cash/Bank')

                WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'EXPENSE'
                     AND NOT EXISTS (
                         SELECT 1 FROM accounts
                         WHERE account_id = NEW.account_id
                           AND category IN ('Cash/Bank', 'Operating Expense')
                     )
                    THEN RAISE(ABORT, 'EXPENSE must affect Cash/Bank or Operating Expense')

                WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'SALARY_PAYMENT'
                     AND NOT EXISTS (
                         SELECT 1 FROM accounts
                         WHERE account_id = NEW.account_id
                           AND category IN ('Cash/Bank', 'Salary')
                     )
                    THEN RAISE(ABORT, 'SALARY_PAYMENT must affect Cash/Bank or Salary')

                WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'OPENING_STATE'
                     AND NOT EXISTS (
                         SELECT 1 FROM accounts
                         WHERE account_id = NEW.account_id
                           AND category IN ('Cash/Bank', 'Customer', 'Vendor')
                     )
                    THEN RAISE(ABORT, 'OPENING_STATE must affect Cash/Bank, Customer, or Vendor')

                WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'OPENING_STATE'
                     AND (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NULL
                     AND NEW.amount <= 0
                    THEN RAISE(ABORT, 'Initial opening balance must be positive')
            END;
        END;
        """.trimIndent(),
    )

    /**
     * Migration 3→4 can be validated as a standalone historical step, before
     * the version-5 tables exist. Current trigger rules that mention those
     * tables are therefore installed only by migration 4→5.
     */
    val version4CompatibleTriggers: List<String>
        get() = createTriggers.filterNot { sql ->
            sql.contains("CREATE TRIGGER IF NOT EXISTS validate_store_profile_") ||
                sql.contains("CREATE TRIGGER IF NOT EXISTS validate_opening_batch") ||
                sql.contains("CREATE TRIGGER IF NOT EXISTS validate_opening_inventory_entry") ||
                sql.contains("CREATE TRIGGER IF NOT EXISTS validate_opening_account_entry") ||
                isDailyCloseTrigger(sql)
        }

    val version5CompatibleTriggers: List<String>
        get() = createTriggers.filterNot(::isDailyCloseTrigger)

    private fun isDailyCloseTrigger(sql: String): Boolean =
        sql.contains("CREATE TRIGGER IF NOT EXISTS validate_daily_close_action") ||
            sql.contains("CREATE TRIGGER IF NOT EXISTS prevent_closed_")

    val seedStatements: List<String> = listOf(
        seedProduct("Milk", "Liters"),
        seedProduct("Yogurt", "Kg"),
        seedAccount("Cash", "Asset", "Cash/Bank"),
        seedAccount("JazzCash", "Asset", "Cash/Bank"),
        seedAccount("Bank", "Asset", "Cash/Bank"),
        seedAccount("Expense: Rent", "Expense", "Operating Expense"),
        seedAccount("Expense: Electricity", "Expense", "Operating Expense"),
        seedAccount("Expense: Internet", "Expense", "Operating Expense"),
        seedAccount("Expense: Water", "Expense", "Operating Expense"),
        seedAccount("Expense: Fuel", "Expense", "Operating Expense"),
        seedAccount("Expense: Poly bags", "Expense", "Operating Expense"),
        seedAccount("Expense: LPG", "Expense", "Operating Expense"),
        seedAccount("Expense: Other", "Expense", "Operating Expense"),
        "INSERT INTO settings (setting_key, setting_value, updated_at) VALUES ('milk_rate_vdr', 170.0, CURRENT_TIMESTAMP)",
        "INSERT INTO settings (setting_key, setting_value, updated_at) VALUES ('delivery_fee_vdr', 11.0, CURRENT_TIMESTAMP)",
        "INSERT INTO settings (setting_key, setting_value, updated_at) VALUES ('ts_standard', 13.00, CURRENT_TIMESTAMP)",
        "INSERT INTO settings (setting_key, setting_value, updated_at) VALUES ('retail_rate_milk', 190.0, CURRENT_TIMESTAMP)",
        "INSERT INTO settings (setting_key, setting_value, updated_at) VALUES ('retail_rate_yogurt', 280.0, CURRENT_TIMESTAMP)",
        "INSERT INTO settings (setting_key, setting_value, updated_at) VALUES ('default_delivery_surcharge', 50.0, CURRENT_TIMESTAMP)",
    )

    /**
     * Seed rows carry fixed well-known uids so every fresh installation in
     * every lineage agrees on their identity before the first sync runs.
     */
    private fun seedProduct(name: String, unit: String): String {
        val uid = LegacyUid.seedProductUid(name, unit)
            ?: error("Unknown seed product identity: $name")
        return "INSERT INTO products (name, unit, uid) VALUES ('$name', '$unit', '$uid')"
    }

    private fun seedAccount(name: String, type: String, category: String): String {
        val uid = LegacyUid.seedAccountUid(name, category)
            ?: error("Unknown seed account identity: $name")
        return "INSERT INTO accounts (name, type, category, uid) VALUES ('$name', '$type', '$category', '$uid')"
    }
}
