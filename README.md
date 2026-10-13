# QBit Data Analysis

Statistical analysis and data dives on any QQQ table, filter or saved view: profiles, pivots, comparisons, anomalies, and saved and scheduled analyses. The approved [design is QRun-IO/qqq#1137](https://github.com/QRun-IO/qqq/pull/1137), `docs/DATA-ANALYSIS.md` in QQQ; [DESIGN.md](docs/DESIGN.md) includes its phase table.

Implemented now: repository scaffold, producer and configuration, six visible tables, enum sources, joins and child lists, permissions and owner locks, PostgreSQL DDL generation, and the Analyses home with real counts. No analysis engine, Analyze action, processes, scheduling, alert evaluation, exports or UI API endpoints ship yet.

## Requirements

- Java 21; QQQ **4.1.0-RC.5+**.
- A configured QQQ backend for the qbit tables and authenticated sessions with permissions and security keys.
- Later phases require the core aggregate extensions in DATA-ANALYSIS.md §3 and the UI API extension point ([qqq#1128](https://github.com/QRun-IO/qqq/pull/1128), RC.6).

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

Only Saved Analyses, Watches, Analysis Settings and Analysis Runs appear in navigation. Schedules, stars and linked watches appear on saved records. Enum PVS cover analysis kind, chart kind, detector, watch state, run status, schedule frequency and format. JSON definitions, requests and results are stored as text; typed request validation arrives with the relevant execution phase.

## Permissions

| Permission | Grants |
| --- | --- |
| `data_analysis.saved.read`, `.insert`, `.edit`, `.delete` | Corresponding actions on saved analyses, stars and schedules |
| `data_analysis.watch.read`, `.insert`, `.edit`, `.delete` | Corresponding watch actions |
| `data_analysis.settings.read`, `.insert`, `.edit`, `.delete` | Analysis Settings (per-table configuration) |
| `data_analysis.run.read`, `.insert`, `.edit`, `.delete` | Analysis Runs (run history and cached results, still subject to the user lock) |
| `data_analysis.admin` | The all-access key of the `data_analysis_user` record locks (see Security) |
| `<effective table permission base>.analyze` | Analyze a source table, also requiring its READ permission |

`DataAnalysisPermissions.analyzePermissionName(table)` honors a custom permission base name and instance defaults. `canAnalyze(table)` requires both READ and the analyze permission; if instance core permission enforcement is off, only the READ check is waived. The feature check is a stand-in for core `TableFeaturePermissionsMetaData` (§3.6); hosts must grant these analyze names explicitly until that hook registers them in the catalog. All qbit tables use standard permission rules (no custom checker), so every name above appears in the core permission catalog and the qbit-authentik policy editor.

## Security

Every home count runs under the current `QContext` session with `PermissionEnforcement.ENFORCE`. A denied section explains the missing permission instead of showing zero. Mine and starred counts explicitly filter the current user even for administrators. Shared counts include only readable shared rows. Stars pointing at unreadable or deleted saved analyses do not count. Run counts cover starts within the last seven days, excluding future rows.

Saved analyses have a READ_AND_WRITE lock on `owner_user_id`; stars and runs have locks on `user_id`. Schedules inherit the saved analysis owner's lock through their join. Null owners are denied. The host's authentication/session adapter must supply:

```java
session.withSecurityKeyValue("data_analysis_user", session.getUser().getIdReference());
// Only after the host has authenticated and authorized an administrator:
session.withPermission("data_analysis.admin")
   .withSecurityKeyValue("data_analysis.admin", true);
```

QQQ permissions and security-key values are separate. An admin permission alone does not bypass record locks. User-id fields must use the same stable string identity as `QUser.idReference`.

**Shared READ is deliberately restricted in this scaffold.** The ordinary owner lock does not express owner OR shared while keeping writes owner-only, so setting `shared=true` currently does not expose a saved analysis to other users. Phase f will introduce separate read and write policies: reads permit owner OR shared only after checking `data_analysis.saved.read`; writes retain owner/admin checks. Query, get, count, joins, child lists and exports will receive cross-user regression tests before enabling shared READ. Collaboration sharing and favorites (§7.3, qqq#1136) will then replace the local sharing/star contracts. Shared analyses must always execute as their viewer.

Minimum group size defaults to 0 (off). Hosts should configure it for personal-data tables. Privacy suppression and outlier restrictions are planned with the analysis engine; the scaffold does not compute or cache analysis results.

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

All properties have fluent `withX` setters. Global numeric defaults must be non-null and non-negative. Nullable per-table numeric overrides inherit global defaults; configured values must be non-negative. `TableAnalysisDefaults` also supports enabled, analyzable fields and excluded fields. Runtime table-config rows will take precedence over code defaults through the specified 60-second lookup in a later phase; the repository phase defines the configuration contract only.

## Adopting in another app

1. Add a backend, install the producer and run the usual `QInstanceValidator`.
2. For PostgreSQL, obtain `DataAnalysisSchema.postgresScript(config)` and apply it through your application's migration system. It derives fields and unique indexes from table metadata. It does not execute DDL or upgrade existing schemas.
3. Map user-id security keys and permissions in the authentication adapter. Configure core audit persistence if field audit history is needed.
4. Review per-table include/exclude sets, limits and minimum group sizes before enabling future computation. Integrate Saved Analyses and Watches with host ESB/Quick Search when those optional host features are installed; this scaffold does not add those dependencies.
5. Use the Analyses home as the owner of analysis data. Other dashboards should link there until saved-analysis widgets are implemented.

Planned phases: **a** core aggregate extensions; **b** profiles, drill-through, runs/cache and endpoints; **c** pivots; **d** comparisons and relationships; **e** watch evaluation and alerts; **f** saved-analysis execution, sharing, stars integration, schedules, chart widgets, full home and exports. The current home summarizes stored rows; it does not run analyses or send deliveries.

## Build

```bash
./mvnw verify
```

The wrapper pins Maven 3.9.11. On Windows use `mvnw.cmd`. Verification runs Checkstyle, JUnit 5/AssertJ tests against the memory backend and JaCoCo gates of 70% instructions and 90% classes. For a populated local dependency cache use `./mvnw -o -q verify`. RDBMS, H2, Testcontainers PostgreSQL and the PostgreSQL driver remain test-scoped for later pushdown tests; middleware Javalin is optional.

## License

[Apache License 2.0](LICENSE). Copyright 2026 Kingsrook, LLC. See [NOTICE](NOTICE).
