# Changelog

Following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Phase b host engine: typed column/table profiles, distinct pages, duplicates, count-equivalent drills, chart data, candidate keys and completeness heatmaps.
- Internal `AnalysisBackend` port and RC.5 stand-in with aggregate pushdown, bounded query pages, deterministic prefix estimates and honest truncation metadata.
- Personalized field/table authorization, enforced FK label lookup, runtime configuration overrides and minimum-group-size privacy before caching.
- Owner-scoped exact runs, asynchronous session propagation, normalized SHA-256 request identities and data-version/TTL cache reuse.
- Shared memory/H2/PostgreSQL contract tests for metric references, drill equivalence, permissions, locks, privacy, estimates, jobs and cache behavior. PostgreSQL tests skip when Docker is unavailable.

- Repository scaffold; analysis settings, saved analyses, schedules, stars, watches and runs tables; permissions; Analyses app home.
