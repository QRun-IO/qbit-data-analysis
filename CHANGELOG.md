# Changelog

Following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

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
