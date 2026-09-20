# Phase 14.8.12 — Operations My attendance blueprint

Status: **AUTO-ACCEPTED FOR NATIVE IMPLEMENTATION** (owner: continue without blueprint pause)

Date: 2026-09-20

## Where this checkpoint starts

Operations Work, Receive milk, Make yogurt, Record spoilage, and Active deliveries are connected. Attendance is still a family preview notice.

This checkpoint owns **My attendance** only: linked employee name + today's status + Mark present. It does not implement half-day, absent, edits, other employees, or Operations More.

## What the existing product already guarantees

- Operator has `ATTENDANCE`.
- App users may optionally link to one Salary employee account (`employeeAccountId`).
- `AttendanceService.record` / bridge `recordAttendance` write present/absent/half_day with revision archive.
- Work home already surfaces Unlinked / NotMarked / Marked from the same link.

## Normal path

1. Bottom nav → **Attendance**.
2. Shared green header: My attendance, identity, role, business date, Open/Closed.
3. If unlinked: calm empty state — ask Owner/Administrator to link this account to an employee. No Mark present.
4. If linked and not marked: show employee name + **Mark present**.
5. Mark present locks as Marking… then shows Present for today.
6. If already marked: show the status (Present) without a second primary write.
7. Closed business date blocks mark with guidance.

## Out of scope

- Half-day, absent, notes, other employees
- Operations More
- Launcher migration

## Verification

Facade + Compose contract tests; `qaScreen -Pqa.screen=attendance`; foundation open + mark present. Prove on A06 with assembleDebug → uninstall → install → launch.
