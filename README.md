# OceanBerry_VCRTS

VCRTS is a Swing-based dashboard for client submissions, admin review, vehicle-owner updates, and residency/completion tracking.

## Build

Compile from the project root.

PowerShell:

```powershell
Remove-Item -Recurse -Force out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force out | Out-Null
javac -cp flatlaf-3.7.jar -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
```

Bash:

```bash
rm -rf out && mkdir -p out && find src -name '*.java' -print0 | xargs -0 javac -cp 'flatlaf-3.7.jar' -d out
```

## Run

Launch the GUI client.

PowerShell:

```powershell
java -cp "out;flatlaf-3.7.jar" app.Main
```

Bash:

```bash
java -cp 'out:flatlaf-3.7.jar' app.Main
```

Launch the VC controller server.

PowerShell:

```powershell
java -cp "out;flatlaf-3.7.jar" app.ServerMain
```

Bash:

```bash
java -cp 'out:flatlaf-3.7.jar' app.ServerMain
```

## Presentation Checklist

1. Start the VC controller server with the `app.ServerMain` command.
2. Open a second terminal and launch the GUI client with the `app.Main` command.
3. If MySQL is not available, keep the checked-in text files in the project root so file fallback mode can still handle demo data.
4. Use the admin dashboard to accept or reject pending client and vehicle-owner submissions.

## Storage Modes

- DB-backed mode: if the MySQL JDBC driver is on the classpath and the configured database is reachable, VCRTS reads and writes through MySQL.
- File fallback mode: if the driver or database is unavailable, VCRTS falls back to the project text files such as `users.txt`, `jobs.txt`, `vehicles.txt`, `vcrts_log.txt`, `pending_request.txt`, and `notifications.txt`.

## Database

- Default connection values are defined in [`src/database/DatabaseConnection.java`](src/database/DatabaseConnection.java).
- You can override them with environment variables:
  - `VCRTS_DB_URL`
  - `VCRTS_DB_USERNAME`
  - `VCRTS_DB_PASSWORD`
- The checked-in schema is at [`src/database/schema.sql`](src/database/schema.sql).
