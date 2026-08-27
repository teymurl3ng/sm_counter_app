PRAGMA foreign_keys = ON;

-- Milk Shop — SQL-only foundation
--
-- This file intentionally keeps the existing architecture:
--   products -> physical stock definitions
--   accounts -> cash / vendor / customer / expense / salary buckets
--   events -> business-event headers
--   inventory_log -> physical stock movements
--   financial_log -> money movements
--   settings -> manually maintained rates
--   current_stock / account_balances -> canonical read views
--
-- No Python or other application language is required to create or verify
-- this database schema.
--
-- IMPORTANT:
-- SQLite cannot force an application to wrap several INSERT statements in
-- one transaction. Every application-level business operation must therefore
-- use BEGIN/COMMIT/ROLLBACK so a multi-row event is atomic.

-- ===========================================================================
-- 1. PRODUCTS / INVENTORY
-- ===========================================================================

CREATE TABLE products (
    product_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    unit TEXT NOT NULL CHECK (unit IN ('Liters', 'Kg'))
);

INSERT INTO products (name, unit) VALUES
('Milk', 'Liters'),
('Yogurt', 'Kg');

-- ===========================================================================
-- 2. FINANCIAL ACCOUNTS
-- ===========================================================================

CREATE TABLE accounts (
    account_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    type TEXT NOT NULL CHECK (
        type IN ('Asset', 'Liability', 'Expense')
    ),
    category TEXT NOT NULL CHECK (
        category IN ('Cash/Bank', 'Vendor', 'Customer', 'Operating Expense', 'Salary')
    ),
    CHECK (
        (category = 'Cash/Bank' AND type = 'Asset') OR
        (category = 'Vendor' AND type = 'Liability') OR
        (category = 'Customer' AND type = 'Asset') OR
        (category = 'Operating Expense' AND type = 'Expense') OR
        (category = 'Salary' AND type = 'Expense')
    )
);

INSERT INTO accounts (name, type, category) VALUES
('Cash', 'Asset', 'Cash/Bank'),
('JazzCash', 'Asset', 'Cash/Bank'),
('Bank', 'Asset', 'Cash/Bank');

INSERT INTO accounts (name, type, category) VALUES
('Expense: Rent', 'Expense', 'Operating Expense'),
('Expense: Electricity', 'Expense', 'Operating Expense'),
('Expense: Internet', 'Expense', 'Operating Expense'),
('Expense: Water', 'Expense', 'Operating Expense'),
('Expense: Fuel', 'Expense', 'Operating Expense'),
('Expense: Poly bags', 'Expense', 'Operating Expense'),
('Expense: LPG', 'Expense', 'Operating Expense'),
('Expense: Other', 'Expense', 'Operating Expense');

-- ===========================================================================
-- 3. BUSINESS EVENTS
-- ===========================================================================

CREATE TABLE events (
    event_id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    occurred_at DATE NOT NULL DEFAULT CURRENT_DATE,

    event_type TEXT NOT NULL CHECK (
        event_type IN (
            'PURCHASE_RECEIPT',
            'VENDOR_PAYMENT',
            'PRODUCTION',
            'SALE',
            'CREDIT_COLLECTION',
            'EXPENSE',
            'SALARY_PAYMENT'
        )
    ),

    -- Reporting label only. It must never determine the sales price.
    channel TEXT CHECK (
        channel IS NULL OR channel IN (
            'retail',
            'wholesale',
            'credit-retail',
            'credit-wholesale'
        )
    ),

    -- Self-reference used for append-only corrections/reversals.
    -- A correction is a new event; the original event is never edited away.
    -- Convention (see spec §11): a correction's own fields (amount_owed,
    -- sale_quantity, etc.) hold the corrected/restated TRUTH, not a delta.
    -- The inventory_log/financial_log rows attached to the correction hold
    -- the DELTA needed to move stock/balances from the original (wrong)
    -- state to that truth.
    corrects_event_id INTEGER,

    -- Vendor-purchase inputs and audited calculations (§4).
    raw_liters REAL,
    fat_percentage REAL,
    lr_reading REAL,
    snf_percentage REAL,
    ts_percentage REAL,
    adjusted_liters REAL,
    amount_owed REAL,

    -- Monetary amount for event types whose primary business value is a single
    -- payment/receipt/expense/salary amount (§§5, 8, 9, 10). For corrections,
    -- this field stores the corrected/restated truth, not the delta.
    amount REAL,

    -- Snapshot of the §3 settings actually used to compute amount_owed above.
    -- Settings drift over time ("seasonal, changes slowly"), so the formula
    -- audit in §9 must replay history against the rate that was live at
    -- insert time, not whatever rate happens to be live today.
    milk_rate_used REAL,
    delivery_fee_used REAL,
    ts_standard_used REAL,

    -- Sales inputs/calculations (§7).
    sale_quantity REAL,
    sale_value REAL,
    unit_rate REAL,
    -- Optional delivery surcharge. It may be applied to any sale regardless
    -- of channel or payment method; current practice uses it mainly for
    -- home/far delivery. Channel remains a reporting label only.
    surcharge_applied REAL NOT NULL DEFAULT 0.0,

    notes TEXT,

    FOREIGN KEY (corrects_event_id) REFERENCES events(event_id),

    CHECK (raw_liters IS NULL OR raw_liters > 0),
    CHECK (fat_percentage IS NULL OR fat_percentage >= 0),
    CHECK (lr_reading IS NULL OR lr_reading > 0),
    CHECK (snf_percentage IS NULL OR snf_percentage >= 0),
    CHECK (ts_percentage IS NULL OR ts_percentage >= 0),
    CHECK (adjusted_liters IS NULL OR adjusted_liters > 0),
    CHECK (amount_owed IS NULL OR amount_owed >= 0),
    CHECK (amount IS NULL OR amount > 0),
    CHECK (milk_rate_used IS NULL OR milk_rate_used > 0),
    CHECK (delivery_fee_used IS NULL OR delivery_fee_used > 0),
    CHECK (ts_standard_used IS NULL OR ts_standard_used > 0),
    CHECK (sale_quantity IS NULL OR sale_quantity > 0),
    CHECK (sale_value IS NULL OR sale_value > 0),
    CHECK (unit_rate IS NULL OR unit_rate > 0),
    CHECK (surcharge_applied >= 0),

    -- Purchase events require their three measured inputs.
    CHECK (
        event_type <> 'PURCHASE_RECEIPT' OR
        (raw_liters IS NOT NULL AND fat_percentage IS NOT NULL AND lr_reading IS NOT NULL)
    ),

    -- Purchase audit/calculation fields must exist once a purchase is created,
    -- including the settings snapshot the calculation was made against.
    CHECK (
        event_type <> 'PURCHASE_RECEIPT' OR
        (snf_percentage IS NOT NULL AND ts_percentage IS NOT NULL AND
         adjusted_liters IS NOT NULL AND amount_owed IS NOT NULL AND
         milk_rate_used IS NOT NULL AND delivery_fee_used IS NOT NULL AND
         ts_standard_used IS NOT NULL)
    ),

    -- Sales: the app enters one of quantity/value and computes the other, but
    -- both must end up stored — a permanently-NULL computed side is a bug,
    -- not a valid state.
    CHECK (
        event_type <> 'SALE' OR
        (unit_rate IS NOT NULL AND sale_quantity IS NOT NULL AND sale_value IS NOT NULL)
    ),

    -- The remaining money-only event types keep their business amount on the
    -- event header as the corrected/restated truth for §11 corrections.
    CHECK (
        event_type NOT IN ('VENDOR_PAYMENT', 'CREDIT_COLLECTION', 'EXPENSE', 'SALARY_PAYMENT')
        OR amount IS NOT NULL
    )
);

-- ===========================================================================
-- 4. PHYSICAL INVENTORY LOG
-- ===========================================================================

CREATE TABLE inventory_log (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    quantity REAL NOT NULL CHECK (quantity <> 0),
    FOREIGN KEY (event_id) REFERENCES events(event_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- ===========================================================================
-- 5. FINANCIAL LOG
-- ===========================================================================

CREATE TABLE financial_log (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id INTEGER NOT NULL,
    account_id INTEGER NOT NULL,
    amount REAL NOT NULL CHECK (amount <> 0),
    FOREIGN KEY (event_id) REFERENCES events(event_id),
    FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

-- ===========================================================================
-- 6. SETTINGS
-- ===========================================================================

CREATE TABLE settings (
    setting_key TEXT PRIMARY KEY,
    setting_value REAL NOT NULL CHECK (setting_value > 0),
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO settings (setting_key, setting_value) VALUES
('milk_rate_vdr', 170.0),
('delivery_fee_vdr', 11.0),
('ts_standard', 13.00),
('retail_rate_milk', 190.0),
('retail_rate_yogurt', 280.0),
('default_delivery_surcharge', 50.0);

-- ===========================================================================
-- 7. INTEGRITY TRIGGERS
-- ===========================================================================

-- A correction must be the same event_type as the event it corrects — a
-- SALE can't "correct" an EXPENSE.
CREATE TRIGGER validate_correction_event_type
BEFORE INSERT ON events
FOR EACH ROW
WHEN NEW.corrects_event_id IS NOT NULL
BEGIN
    SELECT CASE
        WHEN (SELECT event_type FROM events WHERE event_id = NEW.corrects_event_id) <> NEW.event_type
            THEN RAISE(ABORT, 'A correction event must share event_type with the event it corrects')
    END;
END;

-- Only milk can be received in a purchase receipt. An original receipt must
-- increase stock; a correction of an earlier receipt (corrects_event_id set)
-- may move either direction, since its job is to fix a wrong original.
CREATE TRIGGER validate_purchase_inventory
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

-- Production consumes milk and produces yogurt; an original batch must not
-- have the wrong direction for either product. A correction of an earlier
-- batch (corrects_event_id set) may move either direction.
CREATE TRIGGER validate_production_inventory
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

-- Sales consume product stock. A correction of an earlier sale
-- (corrects_event_id set) may return stock, e.g. less was actually
-- delivered than originally logged.
CREATE TRIGGER validate_sale_inventory
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

-- No other event type is allowed to create inventory movement.
CREATE TRIGGER reject_unexpected_inventory_event
BEFORE INSERT ON inventory_log
FOR EACH ROW
WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id)
     NOT IN ('PURCHASE_RECEIPT', 'PRODUCTION', 'SALE')
BEGIN
    SELECT RAISE(ABORT, 'This event type cannot create inventory movement');
END;

-- Do not allow stock to become negative.
CREATE TRIGGER prevent_negative_stock
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

-- Financial movement direction/account-category checks.
CREATE TRIGGER validate_financial_movement
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
    END;
END;

-- ===========================================================================
-- 8. READ VIEWS — CANONICAL CURRENT STATE
-- ===========================================================================

CREATE VIEW current_stock AS
SELECT
    p.product_id,
    p.name,
    p.unit,
    COALESCE(SUM(il.quantity), 0) AS quantity_on_hand
FROM products p
LEFT JOIN inventory_log il ON il.product_id = p.product_id
GROUP BY p.product_id, p.name, p.unit;

CREATE VIEW account_balances AS
SELECT
    a.account_id,
    a.name,
    a.type,
    a.category,
    ROUND(COALESCE(SUM(fl.amount), 0), 2) AS balance
FROM accounts a
LEFT JOIN financial_log fl ON fl.account_id = a.account_id
GROUP BY a.account_id, a.name, a.type, a.category;

-- ===========================================================================
-- 9. AUDIT VIEWS FOR SPEC FORMULAS
-- ===========================================================================
-- The specification is the source of truth. These views independently
-- recalculate stored values from the raw inputs, so an implementation can be
-- checked without trusting a second language's copy of the formula.
--
-- The purchase audit replays each event against the milk_rate_used /
-- delivery_fee_used / ts_standard_used SNAPSHOT stored on that event, not
-- against the live settings table — §3's rates drift over time ("seasonal,
-- changes slowly"), so auditing against today's settings would make every
-- past purchase look wrong the moment a rate changes.

CREATE VIEW purchase_calculation_audit AS
SELECT
    e.event_id,
    e.raw_liters,
    e.fat_percentage,
    e.lr_reading,
    e.milk_rate_used,
    e.delivery_fee_used,
    e.ts_standard_used,
    (0.25 * e.lr_reading) + (0.22 * e.fat_percentage) + 0.72 AS expected_snf_percentage,
    e.fat_percentage + ((0.25 * e.lr_reading) + (0.22 * e.fat_percentage) + 0.72) AS expected_ts_percentage,
    e.raw_liters * (
        (
            e.fat_percentage +
            ((0.25 * e.lr_reading) + (0.22 * e.fat_percentage) + 0.72)
        ) / e.ts_standard_used
    ) AS expected_adjusted_liters,
    (
        e.raw_liters * (
            (
                e.fat_percentage +
                ((0.25 * e.lr_reading) + (0.22 * e.fat_percentage) + 0.72)
            ) / e.ts_standard_used
        )
        * e.milk_rate_used
    ) + (
        e.raw_liters * e.delivery_fee_used
    ) AS expected_amount_owed
FROM events e
WHERE e.event_type = 'PURCHASE_RECEIPT';

-- Sales audit: same idea for §7. unit_rate is already stored per-event (the
-- app pre-fills it from settings but it's always editable), so no snapshot
-- column is needed here — the rate actually used IS the stored rate.
CREATE VIEW sale_calculation_audit AS
SELECT
    e.event_id,
    e.sale_quantity,
    e.sale_value,
    e.unit_rate,
    e.surcharge_applied,
    ROUND(e.sale_quantity * e.unit_rate, 2) AS expected_value_from_quantity,
    ROUND(e.sale_value / e.unit_rate, 4) AS expected_quantity_from_value,
    ROUND(e.sale_value + e.surcharge_applied, 2) AS expected_total
FROM events e
WHERE e.event_type = 'SALE';

-- ===========================================================================
-- 10. INDEXES
-- ===========================================================================

CREATE INDEX idx_event_type ON events(event_type);
CREATE INDEX idx_event_occurred_at ON events(occurred_at);
CREATE INDEX idx_inventory_product ON inventory_log(product_id);
CREATE INDEX idx_inventory_event ON inventory_log(event_id);
CREATE INDEX idx_financial_account ON financial_log(account_id);
CREATE INDEX idx_financial_event ON financial_log(event_id);
CREATE INDEX idx_events_corrects_event ON events(corrects_event_id);
