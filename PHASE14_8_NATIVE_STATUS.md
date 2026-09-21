# Phase 14.8 native UI — status overview

Date: 2026-09-21  
Branch: `fix/native-shell-vs-content-owner-work`  
Living canvas: open `native-phase-plan-overview` beside chat

## Snapshot

| Family | Status |
|---|---|
| Cashier (`Sell / Collect / Activity / More`) | **Done** |
| Operations (`Work` + task entries + Deliveries + Attendance + More) | **Done** |
| Owner primary nav (`Overview / Work / Records / More`) | **Done** |
| Owner Work shared entries (Sale…Deliveries + Expense + Pay supplier + Pay salary under Owner shell) | **Done** (CORE METHOD) |
| Owner More deep destinations | **Done** (read slices; writes deferred) |
| Owner Work remaining money / Reports / Manager·Admin | **Left** |

## Done

- **14.8.15A** Shell vs content — CORE METHOD (Cursor rule, unskippable)
- **Cashier** and **Operations** full native preview shells
- **14.8.14** Owner Overview
- **14.8.15** Owner Work home + six Owner-hosted entries (Sale, Collect, Receive, Yogurt, Spoilage, Deliveries)
- **14.8.16** Owner Records — shop-wide list + search + read detail
- **14.8.17** Owner More home — menu + Quick unlock + switch/sign-out
- **14.8.18–21** Products, Customers, Suppliers, Employees — list + read detail under More
- **14.8.22** Costing & plans — policy list + read detail
- **14.8.23–26** Shop settings, Users & roles, Security history, Backup & restore — read panels / lists (batch)
- **14.8.27** Owner Work Record expense — Owner-hosted form + post (CORE METHOD; Cashier host deferred)
- **14.8.28** Owner Work Pay supplier — Owner-hosted form + post (CORE METHOD; Manager/Admin hosts deferred)
- **14.8.29** Owner Work Pay salary — Owner-hosted form + post (CORE METHOD; Manager/Admin hosts deferred)

## Partial / uncommitted

- **14.8.22–26** (Costing through Backup) — implemented + `qaScreen -Pqa.screen=ownermore` **PASS**, **still uncommitted**
- **14.8.27** Record expense — committed as `a4081ed`
- **14.8.28** Pay supplier — implemented; run `qaScreen -Pqa.screen=ownerwork` before commit
- **14.8.29** Pay salary — implemented; run `qaScreen -Pqa.screen=ownerwork` before commit
- Directory create/edit write UIs (Products…Employees, Costing approve, Shop edit, user enroll, backup export/restore/connect) — deferred with preview notices
- Records ledgers / closing history / corrections **write** — deferred from 14.8.16
- Cashier expense host wrapper — deferred from 14.8.27

## Left (preview or not started)

### Owner More

- **Write depth** on already-connected destinations (create/edit/approve/export/restore) — later depth, not menu gaps

### Owner Work money

- Cash count & close (+ reopen later)

### Later depth

- Month reports  
- Manager / Admin native shells  
- Cashier-hosted expense shell  
- Manager / Admin Pay-supplier / Pay-salary hosts  

## Recommended next moves

**Queue B continues** with Cash & close after pay-salary qaScreen PASS.

## Authority

- `PHASE14_8_2_COUNTER_LEDGER_LEADERSHIP.md` / journey / role calibration  
- Owner blueprints `PHASE14_8_14` … `PHASE14_8_29`  
- `.cursor/rules/native-shell-vs-content.mdc`
