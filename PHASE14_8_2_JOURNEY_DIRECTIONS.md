# Phase 14.8.2 — Native journey audit and directions

Status: **DIRECTION DECISION PENDING — Direction A recommended**

Date: 2026-09-12

## Outcome being designed

An owner or staff member should open Saaf Milk and immediately understand:

1. what matters today;
2. how to record the next real-world action;
3. whether the action was saved;
4. where to inspect or correct past records;
5. where infrequent setup, security, and recovery controls live.

The design must work for people who run a shop and do not think in software modules or accounting-system terminology.

## Owner workflow preference

The owner explicitly selected a show-before-build process for this and future major UI decisions: review concrete visual directions first, choose the experience, preview its material states, and only then implement it. Phase 14.8 uses that sequence for the shell and for every later screen family.

## Evidence used

- the final Phase 12/13 screenshot set covering 83 material application states;
- the Phase 14 S24/A06 device evidence and the owner's hands-on feedback;
- the live React route, permission, form, feedback, and navigation source;
- the 91-action bridge contract and Kotlin service ownership inventory;
- Android guidance for Compose, state-driven UI, adaptive navigation, accessibility, Back, system bars, and large text;
- current interaction patterns documented by Shopify POS, Square POS, Loyverse Dashboard/POS, and Zoho Books.

This is an ordinary project design audit. It is not a formal Design Arc review record.

## What the current interface gets right

- Business operations are real and native-backed.
- Role and permission filtering is explicit.
- Labels usually use shop language rather than database terminology.
- Touch targets and type are large enough for the tested devices.
- Financial warnings and backup-versus-sync boundaries are stated honestly.
- Search, filters, corrections, reauthentication, and destructive confirmations exist.
- Every retained action has an error path and native authority.

These strengths are preserved as behavior, not copied as layout.

## Current journey findings

### 1. Dashboard starts with report configuration

The first viewport is dominated by two dates and `Run report`. Current stock appears below it; important balances, results, and recent activity require a long scroll. A user opening the app cannot quickly answer “What needs my attention now?”

### 2. Recording work starts below unrelated summaries

`Daily work` repeats up to eight summary cards before the selected form. A sale form therefore starts below the first viewport and its save action is several screens away. The summaries do not change enough by task to justify blocking the task.

### 3. Fourteen areas are presented too evenly

The bottom bar shows Dashboard, Daily work, Deliveries, Accounts, and More. More opens ten large tiles. History, Cash close, Products, Customers, Suppliers, Employees, Cost & plans, Settings, Access, and Backup look equally frequent even though their real use ranges from many times per day to rare recovery/setup work.

### 4. The fifth navigation item changes identity

Selecting a destination from More changes the fifth tab's icon and label to that destination. The stable meaning of the bottom navigation is lost, and long labels can crowd the bar.

### 5. Repeated chrome consumes the useful viewport

A tall green title/header and sign-out icon repeat on almost every screen. Large radii, cards, segmented controls, and generous padding make individual elements readable but reduce information density and create excessive scrolling.

### 6. Too many nested selection layers

Top-level navigation, a task picker, segmented tabs, filters, cards, dialogs, and long forms can all appear within the same journey. The user repeatedly chooses where to work before doing the work.

### 7. Saved actions do not create a strong completion moment

Success notices live inside long pages and can be above the retained scroll position. Phase 14 fixed one concrete Access scroll case, but the underlying pattern remains. Every native mutation needs an immediate visible result, a concise summary of what changed, and a clear next action.

### 8. Motion and spatial continuity are absent

Areas and task forms replace one another without useful transition or persistent context. Motion should explain destination changes, sheet opening, row expansion, saving, and updated values. It must remain restrained and respect reduced-motion settings.

### 9. Useful states are visually flat

Primary actions, reference numbers, warnings, explanatory text, current status, and secondary history often share similar card weight. The interface is safe but makes the user read nearly everything to identify what matters.

## Benchmark lessons to use selectively

### Shopify POS

Its home smart grid makes frequent actions available as direct tiles and allows the grid to reflect the location's workflow. Saaf should borrow the idea of a small, permission-aware quick-action area. Saaf should not add user customization until real shop use proves it necessary.

Reference: [Shopify POS smart grid](https://help.shopify.com/en/manual/sell-in-person/getting-started/smart-grid)

### Square POS

Square puts checkout behind a clear `Checkout` destination and supports item/category tiles, search, direct addition, and a focused cart. Saaf should borrow the focused task flow: once the user chooses Sale, the interface should concentrate on completing a sale rather than keeping the whole dashboard above the form.

References: [Square cart workflow](https://squareup.com/help/us/en/article/8238-build-your-customer-s-cart-in-the-square-retail-pos-app), [Square item grid](https://squareup.com/help/us/en/article/8334-set-up-item-grid)

### Loyverse

Loyverse Dashboard opens with today's high-value sales information and a short bottom navigation; date changes and deeper breakdown are progressive. Loyverse POS begins sales from a focused sales screen. Saaf should borrow the today-first summary and the separation between a concise overview and detailed reports.

References: [Loyverse Dashboard](https://help.loyverse.com/help/using-dashboard), [Loyverse sale flow](https://help.loyverse.com/help/make-sales)

### Zoho Books

Zoho exposes dashboard, history/search, quick create, settings, and modules as distinct levels. Saaf should borrow quick creation and global recent activity/search. It should avoid reproducing a large accounting-module sidebar on a phone.

Reference: [Zoho Books navigation](https://www.zoho.com/us/books/help/getting-started/navigating-zoho-books.html)

## Direction A — Today, Work, Records, More

**Recommendation: select this direction.**

Stable bottom destinations:

1. **Today** — current status, attention items, frequent quick actions, recent saved record, and end-of-day state;
2. **Work** — all permitted recording tasks, grouped by what the user is doing rather than by implementation module;
3. **Records** — accounts/balances, history, corrections, deliveries, attendance, and closing records;
4. **More** — products, people, costing/plans, shop settings, access/security, and backup/recovery.

Why it fits:

- it gives every user a stable four-place mental model;
- daily work and historical inspection are clearly different;
- rare administration stops competing with routine work;
- permission filtering changes content within stable destinations instead of rearranging the whole app;
- it supports a concise owner dashboard and a simpler staff dashboard;
- it maps cleanly to compact bottom navigation and expanded navigation rail.

Risk:

`Records` can become broad. Its landing screen therefore needs clear groups and global search, with direct links from Today and Work results to the relevant record.

## Direction B — Action hub first

Stable bottom destinations:

1. **Work** — a Shopify-like grid of permitted actions is the home screen;
2. **Activity** — recent records and search;
3. **Status** — stock, balances, reports, close state;
4. **More** — master data, settings, access, and backup.

Strength:

This is fastest for staff whose main job is repetitive entry.

Risk:

Owners lose the immediate “how is the shop doing?” view, and `Status` becomes a mixed dashboard/accounts/report destination. The product promise includes dependable understanding as well as fast entry, so this direction overweights recording speed.

## Direction C — Sales, Stock, Money domains

Stable bottom destinations:

1. **Home**;
2. **Sales**;
3. **Stock**;
4. **Money**;
5. **More**.

Strength:

Domain labels are concrete and can feel familiar to a shop owner.

Risk:

Many real actions cross domains. A credit sale changes stock, customer debt, revenue, and cost. Supplier milk receipt changes stock, cost, and payable balance. Deliveries and corrections also span categories. Users may hesitate between tabs, and the architecture would pressure the UI to duplicate actions.

## Recommended native information architecture

### Today

- compact shop/user header with account access in a profile menu;
- current business date and close status;
- permission-aware quick actions: Record sale, Receive milk, Payment, and More work;
- only the four to six current facts that change a decision;
- attention list for negative/unusual states already known by native data;
- most recent saved activity with a direct detail link;
- owner-only concise result summary, with date/report configuration on a separate Reports screen.

### Work

- **Sell and collect:** Sale, customer collection;
- **Buy and pay:** Receive milk/purchase, supplier payment;
- **Make and move stock:** Production, spoilage, delivery;
- **Shop spending:** Expense, salary;
- show only permitted tasks;
- remember the last task as a convenience, while always keeping the task list one Back action away;
- once a task opens, remove dashboard cards and focus on the form.

### Records

- global recent activity and search;
- customer and supplier balances with ledger drill-down;
- delivery status/history;
- attendance records;
- cash count, close, and reopen history;
- corrections begin from a record detail, preserving original values and audit lineage.

### More

- **Things you sell:** Products;
- **People and businesses:** Customers, suppliers, employees;
- **Planning and setup:** Costing/plans, shop settings, opening state, expense categories;
- **Your account and staff access:** passphrase, Quick unlock, users, roles, security history;
- **Safety and recovery:** local backup, Drive backup, restore;
- sections appear only when the user has at least one relevant capability.

## Native interaction rules

- Bottom destinations never change label or order for an active session.
- A role may see reduced content, but the meaning of Today, Work, Records, and More remains stable.
- A recording form has one primary job and no unrelated summary wall above it.
- Save keeps the user in context and shows a native completion sheet/snackbar with record number, essential result, and `View record`/`Record another` choices.
- Forms validate near the field and keep a concise error summary when needed.
- Back closes the current layer or returns one level; a dirty draft receives one native discard decision.
- Reports use fast date presets first, with custom dates behind an explicit choice.
- Search expands only where it is useful and does not reserve a large empty field on every screen.
- Motion communicates navigation, expansion, saving, and refreshed values; it does not delay entry.
- Large text reflows; labels and controls never depend on horizontal scrolling.

## Direction decision

Approve Direction A to make `Today / Work / Records / More` the stable native structure. Approval authorizes detailed journey wireframes and the Compose foundation; it does not authorize changing business rules, database schema, security behavior, backup formats, or cloud scope.
