# Data Analysis design

Approved specification: [QQQ docs/DATA-ANALYSIS.md](https://github.com/QRun-IO/qqq/blob/develop/docs/DATA-ANALYSIS.md), introduced in [PR #1137](https://github.com/QRun-IO/qqq/pull/1137).

Data Analysis will provide profiles, pivots, comparisons, relationships and anomaly watches on existing QQQ data. It will push supported computation to the backend, use bounded streamed fallbacks, enforce the viewer’s permissions and privacy limits, and store configuration, saved definitions, schedules and run history in ordinary visible tables. Backend phases b–f are implemented here: typed analysis engines, saved/shared/starred definitions, schedules, exports, SQL handoff and standard widgets with session-scoped data. Specialized analysis frontend work lives in Next. README documents the required shared-read migration and the installed core transport limitations.

## Phases (spec §11)

| Phase | Repo(s) | Deliverable |
|---|---|---|
| spec | qqq | this document |
| repo | qbit-data-analysis | repo standard, scaffold, producer, config, tables, permissions |
| a | qqq | §3: operators, buckets, capabilities, streamed fallback, sampling, feature-permission hook; memory + RDBMS (Postgres via Testcontainers, H2) |
| b | qbit, Next, (qbit-authentik) | §4 profiles + drill-through + runs/cache + §8 endpoints for them; column-header action and profile panel; authentik catalog FEATURE action |
| c | qbit, Next | §5 pivot explorer |
| d | qbit, Next | §6 compare, shift, correlation, crosstab |
| e | qbit | §7.6 watches and alerts |
| f | qbit, Next | §7 saved, stars, schedules, widgets, Analyses home, export |
