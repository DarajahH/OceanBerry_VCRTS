# OceanBerry_VCRTS

VCRTS is a Swing-based dashboard for client submissions, admin review, vehicle-owner updates, and residency/completion tracking.

## Build

Compile from the project root:

```bash
rm -rf out && mkdir -p out && find src -name '*.java' -print0 | xargs -0 javac -cp 'flatlaf-3.7.jar' -d out
```

## Run

Launch the GUI client:

```bash
java -cp 'out:flatlaf-3.7.jar' app.Main
```

Launch the VC controller server:

```bash
java -cp 'out:flatlaf-3.7.jar' app.ServerMain
```

## Storage Modes

- DB-backed mode: if the MySQL JDBC driver is on the classpath and the configured database is reachable, VCRTS reads and writes through MySQL.
- File fallback mode: if the driver or database is unavailable, VCRTS falls back to the project text files such as `users.txt`, `jobs.txt`, `vehicles.txt`, `vcrts_log.txt`, `pending_request.txt`, `admin_decision.txt`, and `notifications.txt`.

## Database

- Default connection values are defined in [src/database/DatabaseConnection.java](</Users/naeemhawkins/untitled folder 2/OceanBerry_VCRTS/src/database/DatabaseConnection.java>).
- You can override them with environment variables:
  - `VCRTS_DB_URL`
  - `VCRTS_DB_USERNAME`
  - `VCRTS_DB_PASSWORD`
- The checked-in schema is at [src/database/schema.sql](</Users/naeemhawkins/untitled folder 2/OceanBerry_VCRTS/src/database/schema.sql>).
