# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

**QBit Data Analysis**: statistical analysis and data dives on any table, filter or saved view. Design: [QRun-IO/qqq#1137](https://github.com/QRun-IO/qqq/pull/1137), `docs/DATA-ANALYSIS.md` in QQQ. Repository phase: metadata, permissions and Analyses home summary. Computation and UI phases a-f are planned.

## Build Commands

```bash
./mvnw compile
./mvnw test
./mvnw verify  # Checkstyle, tests, JaCoCo: 70% instructions, 90% classes
```

Always use the wrapper (Maven 3.9.11); Maven 3.10 cannot read the parent POM.

## Architecture

Entry point: `DataAnalysisQBitProducer implements QBitMetaDataProducer<DataAnalysisQBitConfig>` in `com.kingsrook.qbits.dataanalysis`. Component producers are scanned only in `metadata`. Tables live in `model`, widgets in `app`, permission helpers in `permissions`.

## Invariants

- Queries use the current user's session and enforced permissions.
- Saved analyses remain owner-locked; the shared flag does not broaden READ yet. See README Security.
- Hosts populate `data_analysis_user` with the session user id; only administrators get the `data_analysis.admin` all-access security key.
- Real counts only. Schedules and stars are children of saved analyses. No processes in navigation.

## Naming

- Tables: `data_analysis_<name>`; SQL names equal QQQ table names. Table PVS use the table name.
- Permissions: `data_analysis.saved.*`, `data_analysis.watch.*`, `data_analysis.settings.*`, `data_analysis.run.*` (standard rules, no custom checker), `data_analysis.admin` (lock all-access key), plus `<effective table permission base>.analyze` per analyzed table.
- Data lives in visible standard QQQ tables with permissions and record locks.

## Code Style

- Copy the Apache header from existing Java files; 3-space indent, Allman braces, `return (x);`.
- Flower-box Javadoc on every class and method; fluent `withX()` setters.
- Wrapper types in config and entities; JUnit 5 and AssertJ tests.

## Dependencies

Java 21; parent `com.kingsrook:qbit-build-parent:2.0.0`. Default-active `qqq-snapshot` imports QQQ `4.1.0-RC.5` ahead of the parent's 4.0.0 BOM (ADR-0007); remove activation when the parent moves to 4.1. Middleware Javalin is optional. RDBMS, H2, PostgreSQL driver and Testcontainers are test-only for later pushdown tests. Later phases need core aggregate extensions and the UI API extension point (#1128, RC.6).

## Release

CircleCI qqq-orb 0.6.8: `develop` publishes SNAPSHOTs, `release/*` publishes RCs (orb version bumps), `vX.Y.Z` publishes releases. No manual publishing.
