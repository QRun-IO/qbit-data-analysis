# Changelog

Following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Analysis Settings table action **Create rows for analyzable tables**: writes the effective settings as editable `data_analysis_table_config` rows (insert-only, no change in behavior).
- Setup card on the Analyses home (settings, result cache, saved analyses, watches, schedules).
- Help bundle `help/qbit-data-analysis.yaml`: every table says what it is for and how its rows get created.
- `DataAnalysisTablesGuardrailTest` and `TableConfigDefaultsTest` (including a test that a settings row's `cache_ttl_minutes` changes result caching).
- Phase f saved execution for all seven kinds, core owner-or-shared READ with owner-only WRITE, typed definition validation and viewer-specific field reduction.
- Idempotent personal star endpoints and visible saved list; transient viewer runs, core CSV/XLSX downloads and QueryStat-backed SQL Workbench handoff.
- Five-minute owner-session schedule runner, per-recipient FULL access checks and record-scoped reruns, core messaging with attachments, explicit failure statuses/alerts, timezone/weekly/monthly/cron timing and streamed ETL Send now.
- Standard statistics/chart/table dashboard widgets, linked saved analysis widgets and real home rows with narrow tables and honest empty states.
- Extension 1.4.0, seven new assistant-discoverable endpoints, regenerated OpenAPI and memory/H2/HTTP/export/email/security regression coverage.
- Migration requirement: backfill saved `read_owner_user_id` before enabling shared reads and widen schedule `last_status` to TEXT. Stock core EMAIL attachment limitations fail visibly; supported messaging providers receive full attachments.

- Phase e anomaly watches: owner-locked configuration, validated metrics/detector parameters, complete calendar buckets, five pure detectors, and scheduled evaluation through owner sessions.
- Core alerts SPI type `data.analysis.anomaly` (core disallows the spec's underscore), stable watch keys, relative drill links, raise/recovery transitions, and persisted outcomes without an alerts implementation.
- Standard streamed ETL Evaluate now / Enable / Disable record actions and firing-first linked watches on the Analyses home.
- Read-only watch preview HTTP endpoint, semantic chart/baseline annotations, extension 1.3.0 and regenerated OpenAPI fragment.
- Detector edge cases, real-row owner/security/alert transition tests, calendar boundary and action tests, and HTTP preview coverage.

- Phase d compare, shift, correlation, scatter and crosstab service operations and HTTP specs; extension version 1.2.0 with typed `DataAnalysis*V1` schemas and regenerated OpenAPI.
- Pure-Java two-proportion, Welch, chi-square/Cramer V, PSI and Pearson statistics, including regularized incomplete beta/gamma and independent SciPy references.
- Core-pushed numeric moments and batched correlations, sample trend lines, calendar distribution shifts, category pooling, privacy suppression and exact count drills.
- Shared memory/H2/PostgreSQL relationship contracts, forced core streamed-fallback checks, batching checks and HTTP authorization/serialization/drill tests.

- Phase c pivot backend and `POST /qbit/data-analysis/tables/{table}/pivot`, extension version 1.1.0, typed schemas and regenerated OpenAPI fragment.
- Row/column dimensions, calendar/numeric buckets, seven measure operators, sparse cells, exact Other recomputation, totals, privacy suppression and count-equivalent drills.
- Exposed-join dimensions and measures with READ/FULL checks; filter-only drills for to-one paths and explicit null-drill warnings for to-many paths whose joined-row counts can exceed record counts.
- Configurable field-access resolver, cached reflective core tier bridge with legacy fallback, FULL-only capabilities/selections/labels, generated-drill protection and field-access cache fingerprints.
- Shared memory/H2/PostgreSQL pivot contracts, HTTP drills and errors, reflective resolver/fallback and tier-change cache tests.

- Session-bound profile, capability, distinct-value, duplicate and run endpoints, typed schemas and a checked OpenAPI fragment (introduced in extension 1.0.0).
- Core table-feature permission registration and capability-directed aggregate pushdown, with native sampling and portable streamed fallbacks.
- HTTP authorization, drill-to-core-count round trips, async polling, served OpenAPI and assistant discovery tests; H2 streamed and native-compatibility contract variants.

### Changed

- Boolean pivot/crosstab headers now say Yes / No / (Blank). Pivot measures use human labels such as Count, Average Amount, Sum of Amount, Distinct Email, Median Amount and 90th percentile of Amount.
- Watch DDL includes owner/date fields and last value/message; existing databases need an owner/date backfill before enabling watches.

- Scalable estimates sample each native aggregate without primary-key IN lists, scale counts using observed sample sizes, retain exact population totals and unscaled ratios, and drill over the full population. Portable estimates use one primary-key boundary and report unscaled prefix counts.

- Target QQQ 4.1.0-RC.6; local verification uses `-Dqqq.snapshot.version=4.1.0-DATAANALYSIS-SNAPSHOT`.
- Empty-string and temporal drills round-trip through core HTTP filter interpretation.

### Previously added

- Phase b host engine: typed column/table profiles, distinct pages, duplicates, count-equivalent drills, chart data, candidate keys and completeness heatmaps.
- Internal `AnalysisBackend` port and RC.5 stand-in with aggregate pushdown, bounded query pages, deterministic prefix estimates and honest truncation metadata.
- Personalized field/table authorization, enforced FK label lookup, runtime configuration overrides and minimum-group-size privacy before caching.
- Owner-scoped exact runs, asynchronous session propagation, normalized SHA-256 request identities and data-version/TTL cache reuse.
- Shared memory/H2/PostgreSQL contract tests for metric references, drill equivalence, permissions, locks, privacy, estimates, jobs and cache behavior. PostgreSQL tests skip when Docker is unavailable.

- Repository scaffold; analysis settings, saved analyses, schedules, stars, watches and runs tables; permissions; Analyses app home.
