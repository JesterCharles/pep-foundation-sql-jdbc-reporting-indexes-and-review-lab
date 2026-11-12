# Reporting lab: the registrar's term reports

The spring term is under way in the registrar database from the Queries lab. The course coordinator needs a waitlist, a top-students report and a fast course dashboard. You finish three SQL files. The tests run each one against a fresh database on the PostgreSQL server the tests connect to.

```
OPEN the project -> EDIT a TODO -> RUN the tests -> READ the feedback -> STOP when all pass
                        ^                                  |
                        +------------ next task -----------+
```

In words: open the lab project, edit the TODO for one task, run the tests, read what the failing test tells you, and repeat with the next task. Stop when every test passes.

**Time:** about 20 minutes, at most 40 in one sitting. If you pass 40 minutes, note the next failing task and finish it later.

## Quick start

1. **Open** the lab project in VS Code, then open its terminal with **Terminal > New Terminal**. Go into the Maven project folder:

   ```
   cd starter
   ```

   Run every command in this README from this `starter` folder.
2. **Run** the tests:

   ```
   mvn -q -o test
   ```

   **All 3 tests fail at first. That is expected:** each failure is a task you have not done yet.

   If Maven instead says it cannot resolve a dependency, run `mvn -q dependency:go-offline` once (it needs the internet), then run the tests again.
3. **Edit** the file for one task in [Your tasks](#your-tasks).
4. **Read** the feedback: each failing test starts with its task number and names the next thing to fix. Run that task's test command again after each change.
5. **Stop** when the full run `mvn -q -o test` prints no `[ERROR]` lines.

The tests need a PostgreSQL server they can reach, JDK 25 and Maven 3.9 or later ([Prerequisites](#prerequisites)). If a command fails, see [Troubleshooting](#troubleshooting).

## Your tasks

You edit only the three files below, all in `starter/sql/`. Each one has a `TODO` comment that marks where your work goes. After each task, run its test; it is done when the run prints no `[ERROR]` lines. You can do the tasks in any order.

- [ ] **1. SQL201 waitlist:** `sql/waitlist.sql`
  `mvn -q -o test -Dtest='ReportingLabTest#task1Waitlist'`
- [ ] **2. Top two per course:** `sql/top_students.sql`
  `mvn -q -o test -Dtest='ReportingLabTest#task2TopStudents'`
- [ ] **3. Dashboard index:** `sql/report_index.sql`
  `mvn -q -o test -Dtest='ReportingLabTest#task3ReportIndex'`

**Finish:** run all the tests with `mvn -q -o test`. You are done when it prints no `[ERROR]` lines and `target/surefire-reports/registrar.ReportingLabTest.txt` contains:

```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

`WARNING:` lines from Java or Maven, such as `WARNING: A restricted method ...`, are not test failures.

## The data

`../fixtures/schema.sql` is the registrar schema from the Queries lab, and `../fixtures/term-data.sql` holds the spring term: 4 courses, 8 students and 12 enrollments, some still ungraded (NULL). Before every test, the tests rebuild the tables from these two files.

**Try it in psql (optional, not tested).** From `starter`:

```
psql -h localhost -d postgres -c "CREATE DATABASE reporting_scratch;"
psql -h localhost -d reporting_scratch -f ../fixtures/schema.sql
psql -h localhost -d reporting_scratch -f ../fixtures/term-data.sql
psql -h localhost -d reporting_scratch
```

Try your queries at the prompt; `\d enrollments` lists the indexes PostgreSQL already made for the primary key, and `\q` leaves. When you finish, drop it with `psql -h localhost -d postgres -c "DROP DATABASE reporting_scratch;"`.

## Task details

1. **SQL201 waitlist (`sql/waitlist.sql`).** Offer a SQL201 place to every student whose SQL101 grade is at least the SQL101 average grade and who is not already enrolled in SQL201.
   - The file supplies the first named step, `sql101_average`, which computes the average.
   - Add a second step, `sql201_students`, that lists who is already in SQL201, then the final `SELECT`.
   - Columns `id`, `name`, `sql101_grade`, sorted by `sql101_grade` from highest to lowest, then `id`.
   - The test also checks that the file's statement starts with `WITH`.

   <details><summary>Hint</summary>

   In the final `SELECT`, compare each grade with the average through a scalar subquery, and leave out the SQL201 students with `NOT IN` or `NOT EXISTS`.

   </details>

2. **Top two per course (`sql/top_students.sql`).** For every course, the two best graded students. The file supplies a `WITH` step, `ranked`, that already selects each graded enrollment with its student's name.
   - Where the TODO comment is, add a `place` column that numbers each course's rows by grade, highest first. Student id breaks ties, so Alan and Barbara, who both have 88 in SQL101, always come out in the same order.
   - Add the course's average grade, `course_average`, on every row.
   - Below the step, write the outer query that keeps places 1 and 2.
   - Columns `course_code`, `place`, `name`, `grade`, `course_average`, sorted by `course_code`, then `place`.

   <details><summary>Hint</summary>

   Number the rows with `ROW_NUMBER() OVER (PARTITION BY course_code ORDER BY grade DESC, student_id)`. Add the average with `AVG(grade) OVER (PARTITION BY course_code)`, which keeps each row instead of collapsing the group. With `RANK` or `DENSE_RANK` and no tie-breaker, tied students would share a place and a course could show three rows.

   </details>

3. **Dashboard index (`sql/report_index.sql`).** The dashboard runs `SELECT student_id, grade FROM enrollments WHERE course_code = ? ORDER BY grade DESC`. The primary key's index leads with `student_id`, so it cannot hand back one course's rows in grade order, and the plan still needs a Sort step.
   - Write one `CREATE INDEX`, with a name of your choice, so the plan reads one course's rows in grade order.
   - The test runs `EXPLAIN` on the dashboard query and checks that the plan reads the rows from your index in grade order, with no separate Sort step. It first turns sequential scans off (`SET enable_seqscan = off`), because on 12 rows PostgreSQL would rightly prefer reading the whole small table. If the test fails, it prints the plan: read which scan it chose.

   <details><summary>Hint</summary>

   Use a multicolumn B-tree index whose columns match the query: the filtered column first, then the sorted one. In your scratch database, `EXPLAIN` the same query before and after creating the index to see the change.

   </details>

**Starting over (optional):** the shipped version of every file is in the lab's original repository, in the same folder. Copy your attempt somewhere else first if you want to keep it, then paste the shipped text back into the file.

## What the tests do

Each run creates its own database with a unique name such as `reporting_lab_3f9c0a1b7d2e`, runs the tests in it, and drops it. It never touches a database it did not create. While a run is in progress, the database's name is in `starter/target/lab-database.txt`. After setup, the tests run offline and talk only to that PostgreSQL server.

## Prerequisites

| Tool | Version | Check |
| --- | --- | --- |
| PostgreSQL server and `psql` | 18 (16 or later works) | the connection check below |
| JDK | 25 | `java -version` |
| Apache Maven | 3.9 or later | `mvn -v` |

### Connection check

```
psql -h localhost -p 5432 -d postgres -c "SELECT version();"
```

It should print one row starting `PostgreSQL 18` (or 16, 17) without asking for a password.

## Setup

The quick start is all the setup most learners need. Only if the connection check needs other values: the tests read the same environment variables as `psql`. Set the ones you need in the terminal where you run the tests; they last until you close it, so set them again in a new terminal.

| Variable | Meaning | Default |
| --- | --- | --- |
| `PGHOST` | server host name | `localhost` |
| `PGPORT` | server port | `5432` |
| `PGUSER` | a PostgreSQL user allowed to create databases | your login name |
| `PGDATABASE` | an existing database to connect to first | `postgres` |

For example `export PGUSER=postgres` (macOS or Linux) or `$env:PGUSER = "postgres"` (Windows PowerShell).

**If your server asks for a password**, put one line, `localhost:5432:*:postgres:your-password`, in `~/.pgpass` and run `chmod 600 ~/.pgpass` (macOS or Linux), or in `%APPDATA%\postgresql\pgpass.conf` (Windows). Never write a password into a lab file.

## Troubleshooting

| What you see | What to do |
| --- | --- |
| `cd: no such file or directory: starter` (or `Cannot find path`) | The terminal is not in the lab's top folder, the one that holds this `README.md` and the `starter` folder. Change to that folder, then run `cd starter`. |
| `Could not reach PostgreSQL to create the lab database` | Run the [connection check](#connection-check) until it prints the version. |
| `password authentication failed` or `no password was provided` | Set up a [password file](#setup), or set `PGUSER`. |
| `permission denied to create database` | Use a PostgreSQL user that may create databases, such as `postgres`. |
| `No suitable driver found` | Run the test command from `starter`; the PostgreSQL driver comes from `pom.xml`. |
| Maven cannot resolve a dependency offline | Run `mvn -q dependency:go-offline` once with the internet on. |
| A run was interrupted | Drop the database named in `starter/target/lab-database.txt`, for example `psql -h localhost -d postgres -c "DROP DATABASE reporting_lab_3f9c0a1b7d2e WITH (FORCE);"` with the name from the file. |

## Tested with

PostgreSQL 18.3, JDK 25 and Maven 3.9.9 on macOS (arm64), with no-password local connections. Linux and Windows runs have not been recorded yet; the steps follow PostgreSQL's and pgJDBC's documented behaviour there.
