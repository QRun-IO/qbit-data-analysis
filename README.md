# QBit Data Analysis

Statistical analysis and data dives on any QQQ table, filter or saved view: profiles, pivots, comparisons, anomalies, and saved and scheduled analyses. The approved [design is QRun-IO/qqq#1137](https://github.com/QRun-IO/qqq/pull/1137), `docs/DATA-ANALYSIS.md` in QQQ; [DESIGN.md](docs/DESIGN.md) includes its phase table.

## What works now

The host API provides column and table profiles, pivots, comparisons, distribution shifts, correlations, scatter samples, crosstabs, distinct-value pages, duplicate groups, drill-through filters, privacy suppression, and asynchronous exact profile runs with a persistent per-user cache. Metadata, six visible tables, owner locks, PostgreSQL DDL generation and the Analyses home are also available.

Twenty-one HTTP operations are available under `/qqq/v1/qbit/data-analysis` (extension **1.4.0**). Saved execution, shared reads, personal stars, scheduled email, CSV/XLSX exports, SQL Workbench handoff and standard dashboard widgets are implemented. The separate Analyze/pivot/relationship frontend remains outside this repository.

## Requirements

- Java 21; QQQ **4.1.0-RC.6+**.
- A configured QQQ backend for the qbit tables and authenticated sessions with permissions and security keys.
- Core aggregate and table-feature permission APIs ([qqq#1139](https://github.com/QRun-IO/qqq/pull/1139)) and the UI API extension point ([qqq#1128](https://github.com/QRun-IO/qqq/pull/1128)). Middleware Javalin remains optional; the producer registers endpoints only when it is present.

## Install

Use `com.kingsrook.qbits:qbit-data-analysis:0.1.0-SNAPSHOT` once published, or build locally. After adding your backend to the instance, install the producer:

```java
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitConfig;
import com.kingsrook.qbits.dataanalysis.DataAnalysisQBitProducer;

new DataAnalysisQBitProducer()
   .withQBitConfig(new DataAnalysisQBitConfig()
      .withBackendName("rdbms")
      .withAppName("dataAnalysis")
      .withAppLabel("Analyses"))
   .produce(qInstance);
```

The producer installs metadata into the instance. It scans only `com.kingsrook.qbits.dataanalysis.metadata`; helpers and renderers are not scanned as component producers. Set `withParentAppName("admin")` to nest the app under an existing parent.

## Tables

QQQ names and physical SQL table names are identical. `backendName` in configuration identifies the backend connection; it is not a SQL table name. All tables have `id`, `createDate` and `modifyDate`; SQL columns use inferred snake_case names.

| Table | Purpose and navigation |
| --- | --- |
| `data_analysis_table_config` | Analysis Settings: per-table runtime overrides, unique `table_name`; field audit |
| `data_analysis_saved` | Saved Analyses: typed definitions, owner, shared flag; field audit |
| `data_analysis_saved_star` | Stars: child list, unique (`saved_id`, `user_id`) |
| `data_analysis_schedule` | Schedules: child list with cron/frequency, delivery settings and run state |
| `data_analysis_watch` | Watches: metric, detector, parameters and observed state; links to saved analysis |
| `data_analysis_run` | Analysis Runs: exact-run/cache result and history, scoped by user |

Only Saved Analyses, Watches, Analysis Settings and Analysis Runs appear in navigation. Schedules, stars and linked watches appear on saved records. Enum PVS cover analysis kind, chart kind, detector, watch state, run status, schedule frequency and format. JSON definitions and results are stored as text. Profile, distinct-value and duplicate requests have typed Java contracts and are validated by the facade.

The four `table_name` fields use core's `tables` possible-value source; an existing host source is preserved. A watch's `saved_id` options are filtered by its selected `table_name`. Analysis Settings' `analyzable_fields` and `excluded_fields` use a custom field picker scoped to the selected table. Core currently has no multi-select PVS field, so these settings use a single PVS with comma-separated TEXT storage (for example, `amount,status`); the configuration resolver continues to parse those lists. Explicit dependency metadata awaits core's `possibleValueSourceDependsOn` support.

## Permissions

| Permission | Grants |
| --- | --- |
| `data_analysis.saved.read`, `.insert`, `.edit`, `.delete` | Corresponding actions on saved analyses, stars and schedules |
| `data_analysis.watch.read`, `.insert`, `.edit`, `.delete` | Corresponding watch actions |
| `data_analysis.settings.read`, `.insert`, `.edit`, `.delete` | Analysis Settings (per-table configuration) |
| `data_analysis.run.read`, `.insert`, `.edit`, `.delete` | Analysis Runs (run history and cached results, still subject to the user lock) |
| `data_analysis.admin` | The all-access key of the `data_analysis_user` record locks (see Security) |
| `<effective table permission base>.analyze` | Analyze a source table, also requiring its READ permission |

`DataAnalysisPermissions.analyzePermissionName(table)` honors the effective table permission base. `canAnalyze(table)` delegates to core's table-feature permission check. The producer registers `analyze`, labelled **Analyze**, on configured source tables, excluding this qbit's own tables. No CRUD permission implies analyze. Core checks READ as well as the feature grant on protected tables. All analysis queries enforce permissions even when the instance default is off. Qbit CRUD permissions also appear in the core catalog.

## Security

Every engine operation and home count runs under the current `QContext` session with `PermissionEnforcement.ENFORCE`. A denied section explains the missing permission instead of showing zero. Mine and starred counts explicitly filter the current user even for administrators. Shared counts include only readable shared rows. Stars pointing at unreadable or deleted saved analyses do not count. Run counts cover starts within the last seven days, excluding future rows.

Saved analyses have a WRITE lock on `owner_user_id` and a separate READ lock on the hidden `read_owner_user_id` projection. Core pre-insert/update customizers set that projection to the owner for private rows and null for shared rows; the READ lock allows null. This expresses **owner OR shared READ**, while core retains owner-only writes and administrator all-access keys. Stars and runs have locks on `user_id`. Schedules inherit the saved analysis owner's lock through their join. Null owners are denied. The host's authentication/session adapter must supply:

```java
session.withSecurityKeyValue("data_analysis_user", session.getUser().getIdReference());
// Only after the host has authenticated and authorized an administrator:
session.withPermission("data_analysis.admin")
   .withSecurityKeyValue("data_analysis.admin", true);
```

QQQ permissions and security-key values are separate. An admin permission alone does not bypass record locks. User-id fields must use the same stable string identity as `QUser.idReference`.

**Shared READ is enabled through core record locks.** A viewer still needs `data_analysis.saved.read`. Query/get/count, standard CRUD, saved runs, lists and exports use core enforcement. Shared viewers cannot edit, delete, change ownership, or schedule another owner's analysis. No custom query filter grants access. Sharing exposes the saved definition and metadata, not the owner's data: every run computes under the viewer's source-table READ/Analyze permissions and field policy. There is no per-recipient/group sharing or access-request workflow yet; collaboration's sharing contract ([qqq#1136](https://github.com/QRun-IO/qqq/issues/1136)) will replace the local flag. Stars move to `collaboration_favorite` when collaboration ships.

**Existing databases must migrate before enabling the new metadata.** A missing/null read-owner projection would make an old private row readable. Apply this through the host's migration system while writes are stopped, before deploying this version:

```sql
ALTER TABLE data_analysis_saved ADD COLUMN IF NOT EXISTS read_owner_user_id VARCHAR(250);
UPDATE data_analysis_saved
   SET read_owner_user_id = CASE WHEN shared IS TRUE THEN NULL ELSE owner_user_id END;
ALTER TABLE data_analysis_schedule ALTER COLUMN last_status TYPE TEXT;
```

The metadata-generated create script includes these fields for new installations; it does not migrate existing databases. Fix older placeholder definitions (such as `{}` for a column profile) to valid typed requests. Edit existing enabled schedules through core CRUD to validate recipients and initialize `next_run_at`.

Minimum group size defaults to 0 (off). Hosts should set it for personal-data tables. Groups, bins and distinct listings below N are merged into an Other row with no value or drill. Min/max, percentiles, mean and standard deviation are suppressed when the assessed population is below N. Outlier listings and their count's drill are removed whenever the guard is enabled. The guard runs before chart construction and before `result_json` is stored.

Source fields must be present in personalized metadata, pass `FieldSecurityLockHelper`, and pass the configured allow/exclude lists. FK labels are queried with enforcement before core PVS formatting; unreadable related records or label fields fall back to the source id. This avoids RC.5's ordinary table-PVS translator's trusted permission bypass.

Cached results are scoped to the authenticated user, normalized request, effective policy, session permissions/security keys/backend variants/time zone, and data version. Access and policy are checked again when retrieving runs. Hosts must grant `data_analysis.run.read`, `.insert` and `.edit` to analysis users for persisted profiles and exact jobs. Run errors contain safe text rather than backend data or query details.

## Configuration

| Setting | Default |
| --- | --- |
| `backendName` | Required, must exist in the instance |
| `schedulerName` | Optional; reserved for scheduled delivery |
| `appName`, `appLabel` | `dataAnalysis`, `Analyses` |
| `parentAppName` | None |
| `defaultMaxRows` | 1,000,000 |
| `defaultSampleThresholdRows` | 1,000,000 |
| `defaultSampleRows` | 100,000 |
| `defaultMinGroupSize` | 0 |
| `defaultCacheTtlMinutes` | 60 |
| `includeTables` | Empty set means all tables |
| `excludeTables` | Empty set |
| `tableDefaults` | Empty map of table names to `TableAnalysisDefaults` |

All properties have fluent `withX` setters. Global numeric defaults must be non-null and non-negative. Nullable per-table numeric overrides inherit global defaults; configured values must be non-negative. `TableAnalysisDefaults` also supports enabled, analyzable fields and excluded fields. Runtime `data_analysis_table_config` values override per-table code defaults, which override global defaults. Null row values inherit; blank field lists mean all readable fields or no exclusions. Include/exclude table lists remain an outer restriction, even if a runtime row enables the table. Only the config-row lookup uses a temporary system session, restored in `finally`. It selects configuration fields only and is memoized for 60 seconds. Settings editors can call `service.getConfigResolver().invalidate()` for immediate refresh.

`max_rows` bounds the stand-in's selected population and retained records. An exact request disables sampling but still respects this bound and reports `truncated: true` when necessary; increase the limit to obtain a complete larger profile. Zero selects no rows in the stand-in. Above `sample_threshold_rows`, capable backends run each aggregate with `samplePercent = min(100, sample_rows * 100 / total)` on the user's filter. Counts, top values and histograms are scaled by the full population divided by the observed sample count for that call; ratios and descriptive statistics remain in sample units. The `rows` metric always reports the unsampled total for estimates. Other metrics carry `estimate: true` and an `≈ ` formatted prefix. Sampling never materializes a primary-key IN filter. Backends without sampling use one `pk <= boundary` criterion to compute an unscaled first-N prefix estimate. Estimate drills combine the original filter with the metric condition over the full population, so count/drill equivalence applies only to exact results. Warnings distinguish random samples from prefixes; `sampleRows`, `rowsScanned` and `asOf` describe coverage. `cache_ttl_minutes: 0` disables cache reuse.

## Host API

Call the facade with the authenticated user's `QContext` installed. The producer registers an instance-scoped runtime:

```java
import com.kingsrook.qbits.dataanalysis.DataAnalysisRuntime;
import com.kingsrook.qbits.dataanalysis.result.ColumnProfileRequest;
import com.kingsrook.qbits.dataanalysis.result.TableProfileRequest;
import com.kingsrook.qbits.dataanalysis.result.DistinctValuesRequest;
import com.kingsrook.qbits.dataanalysis.result.DuplicatesRequest;

var service = DataAnalysisRuntime.service();
var capabilities = service.capabilities("orders");
var column = service.columnProfile(new ColumnProfileRequest()
   .withTableName("orders").withFieldName("amount").withFilter(filter));
var table = service.tableProfile(new TableProfileRequest()
   .withTableName("orders").withFilter(filter));
var values = service.distinctValues(new DistinctValuesRequest()
   .withTableName("orders").withFieldName("status").withPage(0).withPageSize(100));
var duplicates = service.duplicates(new DuplicatesRequest()
   .withTableName("orders").withFields(java.util.List.of("customer", "status")));
String runId = service.startExactRun(new ColumnProfileRequest()
   .withTableName("orders").withFieldName("amount").withFilter(filter));
var status = service.getRun(runId); // RUNNING, COMPLETE or FAILED; typed result on COMPLETE
```

`startExactRun` also accepts `TableProfileRequest`. Identical fresh exact requests reuse the existing COMPLETE row and id. A host may construct `DataAnalysisService(config, executor)` to use its managed executor; the default is the common pool. Workers receive a cloned session and instance context, with no inherited transaction or privileged action scope.

The `result` package contains Serializable, Jackson-friendly request/result POJOs, semantic metrics, charts and drills. There are no frontend-specific fields. Distinct pages are zero-based with a maximum size of 500; search matches stored values. Guarded distinct groups are merged before pagination. Shares are fractions from 0 to 1. Numeric standard deviation is the sample statistic; percentiles match `percentile_cont` interpolation at `p * (n - 1)`. Date buckets use the session time zone (including DST), ISO Monday weeks and the specified 92-day/two-year/ten-year thresholds. Date range metrics are in days. Text shapes use at most the first 10,000 rows and state that sample size.

Count drills combine an untouched copy of the request filter with their producing condition. Cardinality drills select one representative record per distinct non-null value; the privacy guard removes those representative drills. Truncated profiles retain their primary-key boundary in drills, so their counts remain reproducible. Serialized drill getters restore temporal operand types from the current instance metadata before core evaluates them.

## HTTP endpoints

Extension `data-analysis` version **1.3.0**, prefix `/qbit/data-analysis` inside `/qqq/v1`:

| Method | Path after `/qqq/v1` | Body or purpose |
| --- | --- | --- |
| GET | `/qbit/data-analysis/tables/{table}/capabilities` | Availability, fields and charts, limits, backend capabilities, SQL Workbench presence |
| POST | `/qbit/data-analysis/tables/{table}/column-profile` | `{fieldName, filter, exact}` |
| POST | `/qbit/data-analysis/tables/{table}/distinct-values` | `{fieldName, filter, page, pageSize, search}` |
| POST | `/qbit/data-analysis/tables/{table}/table-profile` | `{filter, fields}` |
| POST | `/qbit/data-analysis/tables/{table}/duplicates` | `{fields, filter, limit}`; default limit 50, maximum 500 |
| POST | `/qbit/data-analysis/tables/{table}/pivot` | `{rows, columns, values, filter, showTotals, limitGroups}`; 1–3 row dimensions, 0–2 column dimensions, 1–6 measures |
| POST | `/qbit/data-analysis/tables/{table}/compare` | `{filterA, filterB, labelA, labelB, fields}`; null filter selects the whole permitted table |
| POST | `/qbit/data-analysis/tables/{table}/shift` | `{field, dateField, bucket, filter}`; calendar buckets, top 8 categories or quartiles, first/last complete-bucket PSI |
| POST | `/qbit/data-analysis/tables/{table}/correlation` | `{fields, filter}`; 2–12 distinct numeric fields, pairwise r and n, scatter request per cell |
| POST | `/qbit/data-analysis/tables/{table}/scatter` | `{xField, yField, filter, limit}`; default/maximum 2000 points and a sample trend line |
| POST | `/qbit/data-analysis/tables/{table}/crosstab` | `{rowField, columnField, filter, limit}`; default/maximum 20 categories per axis plus Other |
| POST | `/qbit/data-analysis/runs` | `{operation: COLUMN_PROFILE or TABLE_PROFILE, tableName, request}` returns `{runId, status}` |
| GET | `/qbit/data-analysis/runs/{runId}` | Status, timestamps, error, typed `columnProfile` or `tableProfile` |
| POST | `/qbit/data-analysis/watches/{id}/preview` | No body; complete bucket series, `TIME_SERIES` chart with baseline annotations and detector verdict; no writes or alerts |

Every operation uses the request session through `DataAnalysisService`. Capabilities returns 200 with `canAnalyze: false` and no fields when denied. Other permission denials return 403, bad bodies or unavailable fields return 400, and unknown tables return 404. Core middleware supplies plain messages in its standard `{error: message}` JSON envelope. Run rows remain owner-locked.

Requests and drills reuse core's `QueryFilter` JSON shape and schema reference. Versioned `DataAnalysis...V1` schemas cover profiles, metrics, drills, charts, series, points, field summaries, distinct and duplicate results, capabilities and runs. The [OpenAPI fragment](src/main/resources/openapi/qqq-v1/data-analysis.yaml) is checked against regeneration. The deployed `/qqq/v1/openapi.json` and `UiApiInvoker.listOperations()` include all fourteen operations.

Regenerate deliberately after an API change:

```bash
./mvnw -o -q test -Dtest=DataAnalysisApiTest#contract -Dopenapi.write=true -Dqqq.snapshot.version=4.1.0-DATAANALYSIS-SNAPSHOT
```

## Pivot backend and field access

`DataAnalysisService.pivot(PivotRequest)` returns sparse cells indexed into row/column header tuples, aligned totals, typed/formatted metrics, warnings and `asOf`. `COUNT` counts records (including null measure values); other operators require a field. `PERCENTILE` uses continuous interpolation with a fraction from 0 to 1. Calendar buckets use the session timezone. Numeric bins reuse profile nice-edge selection when explicit bounds are omitted; the last interior bin includes its maximum.

The planner first requests count-ordered top-N groups for every dimension (default 50, maximum 200), then dimension counts for privacy/cardinality, one grouped matrix and one aggregate per totals level. Additional aggregates recompute merged Other cells/totals: averages, distinct counts and percentiles cannot safely be combined from displayed cells. Ordinary Other predicates exclude kept values and handle null/empty strings separately. Boolean, temporal and bucketed Other groups use equivalent membership/range predicates for core HTTP compatibility. Cyclic date predicates enumerate observed values through core grouping; memory numeric boundaries reuse the profiles' decimal-scale compatibility helper. Native pivots are exact; bounded core streamed fallback marks partial results as estimates and disables their drills.

`joinTable.field` resolves only through an unambiguous exposed join path. Every table in that path requires READ and the selected field must be FULL; core enforces joins and record locks. The installed data-analysis snapshot does not contain the related-record picker path API, so this implementation uses `getExposedJoins()` and `QueryJoin`. Paths whose every hop is MANY_TO_ONE or ONE_TO_ONE in the analyzed direction support ordinary filter-only drills: core infers the joins from qualified criteria. To-many paths can multiply a base record, so their pivots retain null drills and warn that counts include one row per related table record.

`min_group_size` suppresses rare dimension groups and rare row/column tuples; every measure of a small cell or total is suppressed, including COUNT, with no drill. Totals include the full authorized population. As with existing profiles, separate aggregate calls are not a transactional snapshot.

Hosts can supply `withFieldAnalysisAccessResolver(() -> resolver)`. FULL fields alone appear in capabilities and may be selected. EXACT_MATCH_ONLY and NONE get the same 400 as an unknown field; user-authored filters are passed through for core enforcement. Generated drills using a restricted field (including representative primary keys) are disabled. Related PVS display labels also require FULL access. Exact-run cache identities hash the sorted FULL-field list, the optional core access fingerprint, and related label access.

The default resolver discovers the optional core `fieldaccess.FieldAccessHelper` class and its static methods once. FULL maps to FULL, REVEALABLE to EXACT_MATCH_ONLY, MASKED/HIDDEN to NONE. Without that class, legacy field locks supply FULL/NONE. Replace the reflective calls when the core package ships. Reflection tests use a local stand-in helper rather than shadowing a future real core class at its FQN; the installed snapshot exercises the actual fallback selection. Exact-run persistence continues to support profiles only; pivot results are synchronous.

## Backend selection

`DataAnalysisService` selects `CoreAnalysisBackend` when the source backend advertises aggregate capabilities, including memory. Supported standard deviation, percentiles, null/blank counts, numeric/date buckets and length statistics execute through core aggregates as the current session. Unsupported primitives delegate to `StandInAnalysisBackend`. Patterns and derived length histograms remain streamed; representative distinct drills and cyclic-date membership use bounded scans.

The installed snapshot requires H2 PostgreSQL compatibility mode for native bucket SQL (`GROUP BY` positions).

`engine.AnalysisBackend` isolates profilers from backend-specific operators. `StandInAnalysisBackend` pushes down COUNT, COUNT_DISTINCT, MIN, MAX, AVG, SUM, grouped counts, aggregate ordering and limits through core actions. RC.5 has no aggregate OFFSET, so pages over-fetch through the requested end and slice locally. Memory grouping pagination and scale-sensitive decimal equality have isolated compatibility handling.

Variance, interpolated percentiles, string lengths/shapes, date classification, whitespace detection and representative-row drills use bounded primary-key query pages. Histogram/date membership counts use enforced range/count queries after their boundaries have been calculated. Numeric histograms have 20 interior bins between p1 and p99 plus below/above bins, and the last interior bin is closed. Profiles include Tukey outliers, date gaps, date-time weekday/hour patterns, chart alternatives, a seven-bucket moving average, table completeness and candidate keys (single fields and pairs among the eight most unique).

Data versions use filtered COUNT plus MAX of a readable modification-date-like field, otherwise MAX(primary key). This is the specified inexpensive invalidation heuristic, not snapshot isolation; updates on a table without a modification timestamp can remain cached until TTL expiry. Concurrent writes can also change drill counts after a profile is produced. Results always retain `asOf`.

## Compare and relationships

All five operations resolve FULL fields before reading source data and use core actions with `PermissionEnforcement.ENFORCE`. Core applies table permissions and record locks. Compare batches numeric COUNT, AVG, VAR_SAMP and quartiles per population; categorical comparisons use a common top-10-plus-Other distribution and pool categories with expected counts below five. Deltas are B minus A, and percent deltas divide by absolute A (null when A is zero). Significance uses a strict p < 0.05 threshold; invalid or privacy-suppressed tests return a plain reason.

Correlation batches all CORR pairs into one aggregate call. Pairwise non-null n values use enforced core count actions, since the current aggregate API has no per-measure filter. Unsupported aggregates use core's bounded streamed fallback; a truncated fallback returns 400 with a request to narrow the filter, rather than publishing a partial significance test. Scatter uses the first N paired records ordered by primary key because the current core QueryInput has no sampling API. Its least-squares line describes that displayed sample. Point drills contain only primary-key equality and are disabled whenever privacy is enabled or the primary key is not FULL.

Shift uses session-timezone calendar buckets. Only buckets whose end has passed enter PSI; categories share one top-8-plus-Other basis, and numeric PSI uses the first complete bucket's quartile boundaries for both populations. Zero shares are floored at 0.000001. PSI is descriptive, not a significance test. Category time series explicitly declare `stacked: true`.

The minimum-group-size guard removes small numeric summaries, suppresses crosstab cells and revealing marginal shares, disables drills on privacy-merged Other groups, and suppresses scatter points below the minimum. Statistical tests and PSI are unavailable when protected values would contribute. No runtime statistics dependency is added; beta/gamma, Welch, chi-square, proportions, PSI and Pearson are pure Java, checked against independently computed SciPy references.

## Anomaly watches

Watches are ordinary owner-locked `data_analysis_watch` rows. Create and edit them through core table CRUD with `data_analysis.watch.read`, `.insert`, `.edit`, and `.delete`. Standard table/record actions **Evaluate now**, **Enable**, and **Disable** use streamed ETL Review and Result summaries. They never appear in navigation. The home lists firing watches first and links to their records.

A watch has `owner_user_id`, `date_field`, `bucket` (`HOUR`, `DAY`, `WEEK`, `MONTH`), `detector`, `parameters_json`, `severity` (`INFO`, `WARNING`, `ERROR`, `CRITICAL`) and `enabled`. It targets either:

- `table_name`, optional core `filter_json`, and `metric_json`, such as `{"operator":"COUNT"}`, `{"operator":"SUM","field":"amount"}`, `{"operator":"AVG","field":"amount"}`, `{"operator":"NULL_SHARE","field":"email"}`, or `{"operator":"CATEGORY_SET","field":"status"}`.
- `saved_id`: the owner's saved table and filter, with an explicit watch `metric_json` or one saved definition `metric` / single pivot `values` measure. The date field and bucket remain explicit on the watch. Ambiguous saved measures are rejected; saved filters cannot be overridden by `filter_json` on the watch.

Detector parameters are validated by core pre-insert/update customizers, including partial updates. Unknown parameters and invalid metric/detector combinations return clear record errors.

| Detector | Parameters and behavior |
| --- | --- |
| SPIKE / DROP | `k=14`, `threshold=3.5`, `relativeChange=0.5`; robust z = `0.6745 * (latest - median) / MAD`, strict directional threshold. With zero MAD, compare relative change against the absolute median. A zero baseline fires only for a nonzero change in the requested direction. |
| OUT_OF_RANGE | Required `min` and `max`; fire outside the inclusive range. Needs only the latest bucket. |
| NEW_CATEGORY | `k=14`; requires `CATEGORY_SET`; compare latest categories with the union of previous buckets. Null is excluded from the category set. |
| NULL_RATE_CREEP | `k=14`, `delta=0.05`, `n=3`; requires `NULL_SHARE`; fire for a rise above the baseline median greater than delta, or `n` consecutive increases (`n+1` values). |

`k` and `n` are bounded to 1–1000. The trailing window contains at most `k+1` complete buckets, excludes the current bucket, and follows the owner's time zone (Monday-based weeks). Leading buckets with no observed rows do not invent history. After the first observed bucket, missing COUNT/SUM/category buckets are zero/empty; AVG and NULL_SHARE with no population are unavailable. Too little history, incomplete streamed results, unavailable fields, and privacy-suppressed buckets never cause a recovery. No suppressed series, categories, values or drills leave the computation. All source reads enforce READ, Analyze, record locks and FULL field access, including filter fields.

`data_analysis_evaluate_watches` is hidden and repeats every 900 seconds when `schedulerName` is configured. Give the scheduler session its standard process permission, `data_analysis_evaluate_watches.hasAccess`. Inventory and status writes use grants restricted to watch rows; source aggregation always switches to the owner. The default integration calls the host authentication module's `createAutomatedSessionForUser`. Hosts can instead configure `withOwnerSessionSupplier(userId -> session)`. The session must identify that owner and include the `data_analysis_user` owner key plus their current permissions. Missing/mismatched sessions record **Owner session unavailable**; there is no system-session fallback. Manual Evaluate now can also evaluate a disabled watch.

A transition from OK to FIRING raises through core `QAlerts`, with the watch record reference and a relative table-query `AlertLink`; recovery resolves the same `data_analysis_watch:<id>` key. The declared type is **`data.analysis.anomaly`**, labelled **Data anomaly**. This spelling is necessary because the supplied core SPI validator rejects the design's `data_analysis.anomaly` underscore. No qbit-alerts dependency is required. With no sink installed, core logs the signal and the watch row still records the outcome. Repeated FIRING/OK evaluations do not send duplicate transitions. Calls are serialized within each runtime; hosts should run this scheduler on one node (cross-node transactional coordination is not provided).

`state`, `alert_key`, `last_evaluated_at`, `last_value` and `last_message` describe the most recent attempt. Skips clear the last value and leave the existing OK/FIRING state intact. Preview uses the requesting viewer's permissions and never updates these fields.

`DataAnalysisSchema.postgresScript(config)` generates the new fields and owner requirement. Existing installations must add `date_field VARCHAR(250)`, `owner_user_id VARCHAR(250)`, `last_value NUMERIC`, and `last_message TEXT`, then backfill each watch's actual owner/date field before making the first two columns NOT NULL and enabling evaluation. The CREATE-IF-NOT-EXISTS generator does not migrate existing tables.

## Saved analyses, stars and exports

Create/edit saved analyses through ordinary core table CRUD. `kind` selects `COLUMN_PROFILE`, `TABLE_PROFILE`, `PIVOT`, `COMPARE`, `CORRELATION`, `CROSSTAB` or `SHIFT`; `definition_json` stores that kind's typed request. For example, a column profile has `{"fieldName":"amount","filter":{...}}`, and a pivot has `{"rows":[{"field":"status"}],"values":[{"operator":"COUNT"}]}`. `table_name` identifies the source table; a supplied `tableName` in the JSON must match. The customizers reject malformed JSON, missing selections, nonexistent tables/fields and invalid partial edits with plain record errors. A typed definition filter takes precedence over legacy `filter_json`.

`POST /saved/{id}/run` re-runs as the viewer and returns `{kind, warnings, columnProfile, tableProfile, pivot, compare, correlation, crosstab, shift}`, with the matching typed member populated. Selected fields without FULL analysis access are removed and `Some fields are not available to you` is returned. If a required axis, all required selections, or a filter predicate is unavailable, no computation is performed and the typed members are null; filters are never dropped to broaden the population. Saved profile runs are transient, so they do not require run-table write permission and cannot reuse an owner's cache.

`GET /saved` lists core-visible saved metadata with each viewer's `starred` flag. `PUT /saved/{id}/star` and `DELETE /saved/{id}/star` are idempotent and always target the signed-in user, using standard saved INSERT/DELETE grants. The local star table remains available through ordinary CRUD and has a unique `(saved_id,user_id)` key.

`GET /saved/{id}/export?format=csv|xlsx` re-runs as the viewer. `POST /export` accepts a discriminated request:

```json
{"kind":"PIVOT","tableName":"orders","request":{"rows":[{"field":"status"}],"values":[{"operator":"COUNT"}]},"format":"xlsx"}
```

Both routes return file bytes with attachment headers. Core `ReportFormat` CSV/XLSX streamers write computed result tables: profile metrics/top values, pivot grids, comparisons, correlation/crosstab tables and shift buckets. Values retain their formatting and privacy suppression. Warnings are included in downloads. All paths above are relative to `/qqq/v1/qbit/data-analysis` and appear in assistant operation discovery.

## Scheduled delivery

Configure `schedulerName`, `messagingProviderName`, `fromAddress`, `ownerSessionSupplier` and `recipientAddressSupplier` in `DataAnalysisQBitConfig`. The session supplier resolves both owners and recipients by their stable user id; without it, the authentication module's `createAutomatedSessionForUser` is used. The address supplier maps those ids to verified directory email addresses; schedule recipients are **user ids**, never arbitrary email addresses.

Schedules use `recipients` as a JSON array (1–100 ids), `format` as `HTML_SUMMARY`, `CSV` or `XLSX`, and either Quartz cron (including the seconds field) or `frequency` + `day` + `time` + `time_zone`. Weekly days are ISO Monday=1 through Sunday=7; monthly days are 1–31, clamped to the last day of shorter months. Times are local ISO times such as `09:00`; omitted time zone is UTC. Spring-forward gaps move forward, and the first occurrence is chosen in an autumn overlap. `next_run_at` is strictly after the evaluation time.

The hidden `data_analysis_run_schedules` process repeats every 300 seconds when a scheduler is configured. Its scheduler session needs `data_analysis_run_schedules.hasAccess`. Inventory/status operations use a narrow maintenance session; source computation always switches to the saved owner. Every recipient must resolve to the correct identity and pass source READ, Analyze and FULL access for all selected/filter fields. Different recipients receive a fresh computation under their own record locks. Skipped ids appear in `last_status`.

Email includes escaped HTML headline metrics and a result table (first 20 rows for pivots), with the requested CSV/XLSX attachment via core `SendMessageAction`. `HTML_SUMMARY` has no attachment. Missing messaging configuration and delivery errors persist an explicit failure and raise the declared **`data.analysis.schedule.failed`** alert. Core rejects underscores in alert names, so the spec's `data_analysis.schedule_failed` spelling cannot be registered. With no alert sink, core logs the signal and the status remains visible.

The installed core's stock `EMAIL` provider silently ignores `SendMessageInput.attachmentList`. CSV/XLSX schedules therefore reject that provider with an explicit status/alert; configure an attachment-capable core messaging provider. A test provider verifies the complete message and attachment contract. This qbit does not alter core's SMTP transport.

**Send now** uses the standard streamed ETL Review/Result record action. Reviews do not deliver; execution reauthorizes and reports actual outcomes. Disabled schedules can be sent manually. A runtime serializes scheduled and manual attempts, but there is no cross-node transaction or exactly-once email guarantee; run the scheduler on one node. New configurations compute their next occurrence; failed attempts advance to the next occurrence instead of retrying every five minutes.

## SQL Workbench and dashboard widgets

`POST /tables/{table}/sql` accepts `{kind,request}` and returns `{available,sql,note}`. It requires an installed SQL Workbench app/table and the configured app access permission (default `sql_workbench.hasAccess`), plus source permissions. Memory/non-RDBMS backends report unavailable. The engine executes the request as the viewer and reads the actual main aggregate's core `QueryStat`; grouped dimensions and the widest metric batch identify the main statement. Bound `?` placeholders remain, and the note states that QQQ applies record security at runtime and the SQL is for reading.

Core exposes `QueryStat` on the backend but not the backend reference on `AggregateAction`. A narrow read-only reflective bridge accesses that reference after normal core execution. Query-stat capability is enabled only on the request's cloned table metadata; no global logger or statistics-manager settings change. If core changes that private field or does not provide SQL, the endpoint reports unavailable rather than fabricating a statement.

The Analyses home uses standard `multiStatistics` and `table` widgets: mine/shared/starred counts, enabled/firing watches, my runs in the last seven days, starred analysis links, firing watches and recent run links. All counts and rows use the viewer's session. Tables have at most four columns and use Next's responsive table/card renderer, with explicit empty/permission states.

`SavedAnalysisWidget.widget()` supplies the **`DATA_ANALYSIS_SAVED`** metadata. Clone/rename it for another dashboard and set `.withDefaultValue("savedId", id)`. Its standard chart renderer re-runs as the viewer, shows formatted headline metrics, projects the semantic series/pivot/crosstab into a standard chart, and links back to the saved record. It never displays an owner's cached result. The standard chart is a dashboard summary; the full analysis UI remains responsible for specialized box/heatmap presentations.

## Adopting in another app

1. Add a backend, install the producer and run the usual `QInstanceValidator`.
2. For PostgreSQL, obtain `DataAnalysisSchema.postgresScript(config)` and apply it through your application's migration system. It derives fields and unique indexes from table metadata. It does not execute DDL or upgrade existing schemas.
3. Map user-id security keys and permissions in the authentication adapter. Configure core audit persistence if field audit history is needed.
4. Review per-table include/exclude sets, limits and minimum group sizes before enabling analysis. Integrate Saved Analyses and Watches with host ESB/Quick Search when those optional host features are installed; this scaffold does not add those dependencies.
5. Use the Analyses home as the owner of analysis data. Other dashboards can place a configured DATA_ANALYSIS_SAVED widget linked to its saved record.

Backend phases **b–f** are implemented, including saved/shared/starred analyses, schedules, standard widgets, home, export and SQL handoff. The separate profile, pivot and relationship frontend remains outside this repository.

## Build

```bash
./mvnw -o -q verify -Dqqq.snapshot.version=4.1.0-DATAANALYSIS-SNAPSHOT
```

The wrapper pins Maven 3.9.11. On Windows use `mvnw.cmd`. Verification runs Checkstyle, a shared JUnit 5/AssertJ contract suite on memory and H2, PostgreSQL via Testcontainers when Docker is available, and JaCoCo gates of 70% instructions and 90% classes. The suite checks numerical references, every exact count/drill pair, full-population estimate drills, security, guarded cache JSON, native sampling, asynchronous jobs and cache invalidation. The POM targets RC.6; until its release, always supply the locally installed snapshot override shown above. RDBMS, H2, Testcontainers PostgreSQL and the PostgreSQL driver are test-scoped; middleware Javalin is optional.

## License

[Apache License 2.0](LICENSE). Copyright 2026 Kingsrook, LLC. See [NOTICE](NOTICE).
