# QBit Data Analysis

Statistical analysis and data dives on any QQQ table, filter or saved view: profiles, pivots, comparisons, anomalies, and saved and scheduled analyses. The approved [design is QRun-IO/qqq#1137](https://github.com/QRun-IO/qqq/pull/1137), `docs/DATA-ANALYSIS.md` in QQQ; [DESIGN.md](docs/DESIGN.md) includes its phase table.

## What works now

The host API provides column and table profiles, distinct-value pages, duplicate groups, drill-through filters, effective per-table configuration, privacy suppression, and asynchronous exact runs with a persistent per-user cache. Metadata, six visible tables, owner locks, PostgreSQL DDL generation and the Analyses home are also available.

HTTP endpoints require the QQQ RC.6 extension point and are next, along with the Analyze UI. Pivots, comparisons, anomalies, saved-analysis execution, scheduling and exports remain later phases.

## Requirements

- Java 21; QQQ **4.1.0-RC.5+**.
- A configured QQQ backend for the qbit tables and authenticated sessions with permissions and security keys.
- The engine works on RC.5 through an internal stand-in. A future `CoreAnalysisBackend` can replace it when the core aggregate extensions ship. HTTP endpoints require the UI API extension point ([qqq#1128](https://github.com/QRun-IO/qqq/pull/1128), RC.6).

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

## Permissions

| Permission | Grants |
| --- | --- |
| `data_analysis.saved.read`, `.insert`, `.edit`, `.delete` | Corresponding actions on saved analyses, stars and schedules |
| `data_analysis.watch.read`, `.insert`, `.edit`, `.delete` | Corresponding watch actions |
| `data_analysis.settings.read`, `.insert`, `.edit`, `.delete` | Analysis Settings (per-table configuration) |
| `data_analysis.run.read`, `.insert`, `.edit`, `.delete` | Analysis Runs (run history and cached results, still subject to the user lock) |
| `data_analysis.admin` | The all-access key of the `data_analysis_user` record locks (see Security) |
| `<effective table permission base>.analyze` | Analyze a source table, also requiring its READ permission |

`DataAnalysisPermissions.analyzePermissionName(table)` honors a custom permission base name and instance defaults. `canAnalyze(table)` is the legacy helper; its READ check follows the instance setting. The engine adds an explicit READ check and always uses `PermissionEnforcement.ENFORCE`, even when the instance default is off. The feature check is a stand-in for core `TableFeaturePermissionsMetaData` (§3.6); hosts must grant these analyze names explicitly until that hook registers them in the catalog. All qbit tables use standard permission rules (no custom checker), so their CRUD permissions appear in the core permission catalog and the qbit-authentik policy editor. Hosts explicitly grant source-table analyze permissions until the core feature hook ships.

## Security

Every engine operation and home count runs under the current `QContext` session with `PermissionEnforcement.ENFORCE`. A denied section explains the missing permission instead of showing zero. Mine and starred counts explicitly filter the current user even for administrators. Shared counts include only readable shared rows. Stars pointing at unreadable or deleted saved analyses do not count. Run counts cover starts within the last seven days, excluding future rows.

Saved analyses have a READ_AND_WRITE lock on `owner_user_id`; stars and runs have locks on `user_id`. Schedules inherit the saved analysis owner's lock through their join. Null owners are denied. The host's authentication/session adapter must supply:

```java
session.withSecurityKeyValue("data_analysis_user", session.getUser().getIdReference());
// Only after the host has authenticated and authorized an administrator:
session.withPermission("data_analysis.admin")
   .withSecurityKeyValue("data_analysis.admin", true);
```

QQQ permissions and security-key values are separate. An admin permission alone does not bypass record locks. User-id fields must use the same stable string identity as `QUser.idReference`.

**Shared READ is deliberately restricted in this scaffold.** The ordinary owner lock does not express owner OR shared while keeping writes owner-only, so setting `shared=true` currently does not expose a saved analysis to other users. Phase f will introduce separate read and write policies: reads permit owner OR shared only after checking `data_analysis.saved.read`; writes retain owner/admin checks. Query, get, count, joins, child lists and exports will receive cross-user regression tests before enabling shared READ. Collaboration sharing and favorites (§7.3, qqq#1136) will then replace the local sharing/star contracts. Shared analyses must always execute as their viewer.

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

`max_rows` bounds the stand-in's selected population and retained records. An exact request disables sampling but still respects this bound and reports `truncated: true` when necessary; increase the limit to obtain a complete larger profile. Zero selects no rows. Above `sample_threshold_rows`, estimates use the first `min(sample_rows, max_rows)` rows ordered by primary key. Counts describe that prefix, are not extrapolated, and carry `estimate`, `sampleRows`, `rowsScanned`, warnings and an `asOf` timestamp. This is deterministic prefix sampling, not a random sample. `cache_ttl_minutes: 0` disables cache reuse.

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

## RC.5 stand-in

`engine.AnalysisBackend` isolates profilers from missing core operators. `StandInAnalysisBackend` pushes down COUNT, COUNT_DISTINCT, MIN, MAX, AVG, SUM, grouped counts, aggregate ordering and limits through core actions. RC.5 has no aggregate OFFSET, so pages over-fetch through the requested end and slice locally. Memory grouping pagination and scale-sensitive decimal equality have isolated compatibility handling.

Variance, interpolated percentiles, string lengths/shapes, date classification, whitespace detection and representative-row drills use bounded primary-key query pages. Histogram/date membership counts use enforced range/count queries after their boundaries have been calculated. Numeric histograms have 20 interior bins between p1 and p99 plus below/above bins, and the last interior bin is closed. Profiles include Tukey outliers, date gaps, date-time weekday/hour patterns, chart alternatives, a seven-bucket moving average, table completeness and candidate keys (single fields and pairs among the eight most unique).

Data versions use filtered COUNT plus MAX of a readable modification-date-like field, otherwise MAX(primary key). This is the specified inexpensive invalidation heuristic, not snapshot isolation; updates on a table without a modification timestamp can remain cached until TTL expiry. Concurrent writes can also change drill counts after a profile is produced. Results always retain `asOf`.

## Adopting in another app

1. Add a backend, install the producer and run the usual `QInstanceValidator`.
2. For PostgreSQL, obtain `DataAnalysisSchema.postgresScript(config)` and apply it through your application's migration system. It derives fields and unique indexes from table metadata. It does not execute DDL or upgrade existing schemas.
3. Map user-id security keys and permissions in the authentication adapter. Configure core audit persistence if field audit history is needed.
4. Review per-table include/exclude sets, limits and minimum group sizes before enabling analysis. Integrate Saved Analyses and Watches with host ESB/Quick Search when those optional host features are installed; this scaffold does not add those dependencies.
5. Use the Analyses home as the owner of analysis data. Other dashboards should link there until saved-analysis widgets are implemented.

Phase **b** profiles, drill-through and runs/cache are implemented through the host facade. Remaining work: **a** core aggregate extensions; **b** HTTP endpoints after QQQ RC.6 and analysis UI; **c** pivots; **d** comparisons and relationships; **e** watch evaluation and alerts; **f** saved-analysis execution, sharing, stars integration, schedules, chart widgets, full home and exports. The current home summarizes stored rows; the host facade executes profiles. The home does not send deliveries.

## Build

```bash
./mvnw verify
```

The wrapper pins Maven 3.9.11. On Windows use `mvnw.cmd`. Verification runs Checkstyle, a shared JUnit 5/AssertJ contract suite on memory and H2, PostgreSQL via Testcontainers when Docker is available, and JaCoCo gates of 70% instructions and 90% classes. The suite checks numerical references, every emitted count/drill pair, security, guarded cache JSON, prefix sampling, asynchronous jobs and cache invalidation. For a populated local dependency cache use `./mvnw -o -q verify`. RDBMS, H2, Testcontainers PostgreSQL and the PostgreSQL driver are test-scoped; middleware Javalin is optional.

## License

[Apache License 2.0](LICENSE). Copyright 2026 Kingsrook, LLC. See [NOTICE](NOTICE).
