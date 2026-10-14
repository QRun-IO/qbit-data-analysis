# Upgrading

## Unreleased

No schema change and nothing required of hosts.

- New table action **Create rows for analyzable tables** on Analysis Settings. Run it once if you want the effective settings visible and editable as rows. It changes no behavior, is insert-only, and only adds rows for tables that have none. After a row exists it wins over later changes to the code defaults for that table.
- New **Setup** widget first on the Analyses home, and table help text. Hosts that override help on a table keep their own.
