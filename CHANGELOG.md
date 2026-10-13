# Changelog

Following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- `data-analysis` UI API extension 1.0.0: seven session-bound profile, capability, distinct-value, duplicate and run endpoints, typed schemas and a checked OpenAPI fragment.
- Core table-feature permission registration and capability-directed aggregate pushdown, with native sampling and portable streamed fallbacks.
- HTTP authorization, drill-to-core-count round trips, async polling, served OpenAPI and assistant discovery tests; H2 streamed and native-compatibility contract variants.

### Changed

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
