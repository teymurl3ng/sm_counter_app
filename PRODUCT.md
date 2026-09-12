# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Saaf serves people running a small owner-managed shop on Android phones, including shared phones. The built-in account profiles are Owner, Store manager, Cashier, Operations staff, and Administrator. Owner and Cashier cover the smallest installation; the other profiles appear only when the business delegates those jobs.

Cashier and Operations staff are the least technical and most repetition-sensitive users. Cashiers need rapid, confident counter entry. Operations staff need physical receiving, production, spoilage, delivery, and self-attendance tasks without exposure to financial administration. Owner, Store manager, and Administrator need progressively broader decision, supervision, and administration surfaces.

## Product Purpose

Saaf provides dependable, offline-first daily operational and management records without requiring accounting expertise. Supported users should be able to record work, understand what changed, recover from mistakes, secure a shared phone, and restore protected business data with confidence.

## Positioning

Saaf combines shop-floor work, small-shop money and stock records, role-aware access, audit attribution, and encrypted recovery in one local-first Android product. Its promise is modest supported scope with strong correctness and clear limits rather than enterprise breadth.

## Operating Context

- Cashiers record repeated paid and credit sales and customer collections while a customer is waiting.
- Operations staff record supplier milk measurements, milk-to-yogurt production, spoilage, delivery status, and their own attendance close to the physical work.
- Owners inspect sales, cash, stock, debt, profitability, exceptions, closing state, access, and backup health.
- Store managers run the assigned day, resolve exceptions, manage ordinary records and staff work, and close the day.
- Administrators maintain master data, configuration, ordinary staff accounts, audit, and backups without receiving owner recovery or ownership-transfer powers.
- Phones may be shared, connectivity may be unavailable, and normal entry must remain fast without repeated long-passphrase typing.

## Capabilities and Constraints

- The authoritative business engine is the existing Kotlin/Room implementation with schema version 8, typed services, validation, audit, role security, Quick unlock, encrypted local backup, and Google Drive recovery.
- The intended final presentation is native Android with Jetpack Compose. The React/WebView/bridge presentation is temporary behavioral reference and fallback during migration.
- Daily work is local-first and must not depend on cloud connectivity.
- Milk and Yogurt are the currently validated product domain. Future product expansion requires separate domain, accounting, permission, migration, and test decisions.
- Current saved records include purchases, production, sales, collections and payments, expenses and salaries, spoilage, deliveries, attendance, cash counts, daily close/reopen, corrections, costing, and backup/recovery state.
- Saaf does not currently provide receipt printing, delivery-worker assignment, barcode checkout, integrated payment hardware, tax filing, or statutory financial statements. Design work must not imply those capabilities.
- Permission enforcement and sensitive-data filtering belong at the native service boundary, not only in screen visibility.
- Quick unlock is device-bound convenience using Android phone security or a per-user PIN. Portable restore continues to require the full backup passphrase.

## Brand Commitments

`Saaf Milk` is the current working product name. The product voice is plain, calm, direct, and practical. It uses shop language before accounting language and explains saved results and errors concretely.

The existing React/WebView visual treatment is rejected as the future product experience. Its green-and-cream palette and component styling are historical evidence rather than binding authority for the native redesign. Any future name or broader market positioning requires a separate product decision.

## Evidence on Hand

- A frozen Phase 14 behavioral baseline: 273 ordinary connected Android tests, one clean-install visual audit, seven host tests, bridge contract v5 with 91 actions, and verified backup/restore plus Quick unlock behavior on Samsung S24 and A06 devices.
- Eighty-three retained hybrid screen captures and physical-device accessibility, large-text, keyboard, Back, rotation, split-screen, and recovery findings.
- Native Kotlin commands, read models, validation messages, role permissions, record attribution, and backup/security contracts in the repository.
- Owner hands-on feedback that the current interface is clunky, visually unacceptable, scroll-heavy, confusing, and insufficiently reassuring after actions.
- Current role architecture and detailed Cashier/Operations workflow proposal in Phase 14.8 documents.
- No approved logo asset, product photography, illustration system, or permanent visual identity beyond the working name and voice.

## Product Principles

1. **Daily work first:** repeated tasks open directly, minimize decisions, and never sit below reports or setup content.
2. **Confidence after every action:** saving, success, errors, and recovery are immediate, visible, specific, and preserve safe draft data.
3. **Role truth everywhere:** each role receives the tasks, information, defaults, and approval boundaries needed for its real job.
4. **Correct within honest scope:** supported records and calculations remain dependable; missing coverage is disclosed rather than implied.
5. **Show before building:** material visual and journey decisions are reviewed in complete states before production Compose implementation.

## Accessibility & Inclusion

The product must remain usable by workers with limited software confidence, one-handed users, TalkBack users, and users with increased Android font size. It targets WCAG 2.2 AA contrast, Material 3 Android behavior, 48 dp touch targets, clear focus and error relationships, no color-only meaning, system Back and insets, and layouts that reflow without horizontal scrolling.
