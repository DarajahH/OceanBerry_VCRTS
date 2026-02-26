# OceanBerry_VCRTS
Group 6. Github Repository Main. 

M2 Doc:
https://docs.google.com/document/d/16k-REOj50g5DmJLSiEPO1bqi5zENigJPWOGzb3z3RNc/edit?usp=sharing

## Run (from `Main`)

Compile:
```bash
javac -d bin $(find src -name "*.java")
```

Run:
```bash
java -cp bin app.Main
```

If using FlatLaf jar:
```bash
javac -cp ".:flatlaf-3.7.jar" -d bin $(find src -name "*.java")
java -cp "bin:flatlaf-3.7.jar" app.Main
```

Seeded accounts:
- `admin / 1234` (`ADMIN`)
- `owner / owner123` (`OWNER`)
- `client / client123` (`CLIENT`)
- `jobowner / job123` (`JOB_OWNER`)
