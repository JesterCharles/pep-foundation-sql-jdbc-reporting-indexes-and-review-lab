# Reporting lab: the registrar's term reports

The spring term is under way in the registrar database you seeded in the Queries lab. The coordinator needs a waitlist, a top-students report and a fast course dashboard. You finish three SQL files. Every file you write runs for real against a database on your own PostgreSQL server, through the PostgreSQL JDBC driver that the tests use.

This lab practises *Subqueries and CTEs*, *Window functions: top N per group* and *Indexes*. The JDBC steps of the next module are practised in the JDBC Data Access lab. It needs no cloud account, container or shared server: only your local PostgreSQL server with `psql`, a JDK and Maven, which run the tests.

**Time: at most 40 minutes in one sitting**, day F22's Faded, then fresh problem block. The estimate is about 20 minutes for an average learner: setup 5, Task 1 5, Task 2 4 and Task 3 6. Parts of Tasks 1 and 2 are supplied as starting points (the first named step of the waitlist and the ranked step's rows), so your time goes to the CTE, window and index steps these topics teach. If you pass 40 minutes, pause, note the next failing task, and finish it in self-study.

## What you edit

| File (in `starter/`) | What you do | Why the registrar needs it |
| --- | --- | --- |
| `sql/waitlist.sql` | Task 1: finish one `WITH` query | who should be offered a SQL201 place |
| `sql/top_students.sql` | Task 2: finish one query with window functions | the two best students in each course |
| `sql/report_index.sql` | Task 3: one `CREATE INDEX` | the course dashboard lookup |

Leave `tests/`, `fixtures/`, `expected/`, `starter/pom.xml` and `starter/.gitignore` alone: the tests and your copy's version control read them. The single test command, run in `starter/`, is:

```
mvn -q -o test
```

Each failing check starts with its task number and names the next thing to fix.

## Prerequisites

| Tool | Version | Check |
| --- | --- | --- |
| PostgreSQL server and `psql` | 18 (16 or later works) | the connection check below |
| JDK | 25 | `java -version` |
| Apache Maven | 3.9 or later | `mvn -v` |

**Connection check.** Run this in a new terminal. It must print one row starting `PostgreSQL 18` (or 16, 17) without asking for a password:

```
psql -h localhost -p 5432 -d postgres -c "SELECT version();"
```

## Setup

1. Open the lab. This lab is a self-contained project: the folder that holds this `README.md` is its root, and in your own copy or clone it is the repository root. Open that root folder in VS Code or any other editor; no extension is needed. Run every command in a terminal whose working directory is `starter/`, the Maven project folder inside the root.
2. Only if the connection check needed other settings: set the same values for the tests in this terminal. They last only as long as the terminal, so set them again in every new terminal. They read the variables `psql` reads: `PGHOST` (a host name such as `localhost`, default `localhost`), `PGPORT` (default `5432`), `PGUSER` (a user allowed to create databases, default your login name) and `PGDATABASE` (an existing database, default `postgres`). For example `export PGUSER=postgres` (macOS or Linux) or `$env:PGUSER = "postgres"` (Windows PowerShell). If your server asks for a password, put it in a password file as in the Relational Design lab: one line `localhost:5432:*:postgres:your-password` in `~/.pgpass` (then `chmod 600 ~/.pgpass`) on macOS or Linux, or in `%APPDATA%\postgresql\pgpass.conf` on Windows. Never write a password into a lab file.
3. Download the test libraries once (needs the internet; after the earlier SQL labs, Maven already has most of them):

   ```
   mvn -q dependency:go-offline
   ```

After this step the test command works offline.


## The data

`fixtures/schema.sql` is the registrar schema from the Queries lab, and `fixtures/term-data.sql` holds the spring term: 4 courses, 8 students and 12 enrollments, some still ungraded (NULL). Before every test, the tests rebuild the tables from these two files, so each task can pass on its own and in any order.

**Try it in psql (optional, not checked).** Create a scratch database once with `psql -h localhost -d postgres -c "CREATE DATABASE reporting_scratch;"`, then from `starter/` load it with `psql -h localhost -d reporting_scratch -f ../fixtures/schema.sql` and `psql -h localhost -d reporting_scratch -f ../fixtures/term-data.sql`. Connect with `psql -h localhost -d reporting_scratch` to try your queries; `\d enrollments` lists the indexes PostgreSQL already made for the primary key, and `\q` leaves. Drop it at the end (see Cleanup).

## Tasks

1. **SQL201 waitlist (`sql/waitlist.sql`).** Offer a SQL201 place to every student whose SQL101 grade is at least the SQL101 average grade and who is not already enrolled in SQL201. The file supplies the first named step, `sql101_average`, which computes the average. Add a second step, `sql201_students`, that lists who is already in SQL201, then the final `SELECT`. In it, compare each grade with the average through a scalar subquery, and leave out the SQL201 students with `NOT IN` or `NOT EXISTS`. Columns `id`, `name`, `sql101_grade`, sorted by `sql101_grade` from highest to lowest, then `id`. Done: `task1Waitlist` passes. Besides running your query, this test checks that the file's statement starts with `WITH`; that one part is a static check of the text.
2. **Top two per course (`sql/top_students.sql`).** For every course, the two best graded students. The file supplies a `WITH` step, `ranked`, that already selects each graded enrollment with its student's name. Where the TODO comment is, add a `place` column that numbers each course's rows with `ROW_NUMBER() OVER (PARTITION BY course_code ORDER BY grade DESC, student_id)`: student id breaks ties, so Alan and Barbara, who both have 88 in SQL101, always come out in the same order. Add the course's average grade on every row with `AVG(grade) OVER (PARTITION BY course_code)`, which keeps each row instead of collapsing the group. The step's `WHERE` already leaves out ungraded enrollments before numbering; below the step, write the outer query that keeps places 1 and 2. Columns `course_code`, `place`, `name`, `grade`, `course_average`, sorted by `course_code`, then `place`. Done: `task2TopStudents` passes. (With `RANK` or `DENSE_RANK` and no tie-breaker, tied students would share a place and a course could show three rows.)
3. **Dashboard index (`sql/report_index.sql`).** The coordinator's dashboard runs `SELECT student_id, grade FROM enrollments WHERE course_code = ? ORDER BY grade DESC`. The primary key's index leads with `student_id`. PostgreSQL 18 can sometimes skip through it to reach one course, but it cannot hand back that course's rows in grade order, so the plan still needs a Sort step. Write one `CREATE INDEX` with a name of your choice: a multicolumn B-tree index whose columns match the query, the filtered column first, then the sorted one. Done: `task3ReportIndex` passes, and the whole run then matches Expected output. The test runs `EXPLAIN` on the dashboard query and checks that the plan reads the rows from your index in grade order with no separate Sort step. It first switches sequential scans off (`SET enable_seqscan = off`), because on 12 rows PostgreSQL would rightly prefer reading the whole small table; if the test fails it prints the plan, so read which scan it chose. In your scratch database, `EXPLAIN` the same query before and after creating the index to see the same change.

**Starting over.** Save your attempt elsewhere if you want to keep it, then restore the file you want to restart from a fresh copy of the lab.

## Test

In `starter/`:

```
mvn -q -o test
```

Each run creates its own database with a unique name such as `reporting_lab_3f9c0a1b7d2e`, runs the tests in it, and drops it again. It never removes a database it did not create. While a run is in progress, the database's name is in `starter/target/lab-database.txt`.

As shipped, all three tests fail; that is expected.

## Expected output

When every task is done, `mvn -q -o test` exits with status 0 (`echo $?` on macOS or Linux, `$LASTEXITCODE` in PowerShell, prints `0`) and `starter/target/surefire-reports/registrar.ReportingLabTest.txt` contains the line `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`. Warnings such as `WARNING: A restricted method ...` are not test failures. The full summary is in `expected/test-summary.txt`.

## Offline note

Only the setup step downloads anything. The test command runs offline and talks only to your local PostgreSQL server.

## Cleanup

- The tests drop their own database when they finish.
- After an interrupted run, drop the database named in `starter/target/lab-database.txt`, for example `psql -h localhost -d postgres -c "DROP DATABASE reporting_lab_3f9c0a1b7d2e WITH (FORCE);"`, before you delete `starter/target`.
- If you made the scratch database: `psql -h localhost -d postgres -c "DROP DATABASE reporting_scratch;"`.
- Delete `starter/target` to remove compiled test classes and test reports. The libraries in your local Maven repository (`~/.m2/repository`) are kept for later labs.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `Could not reach PostgreSQL to create the lab database` | Run the connection check with the same settings until it prints the version. |
| `password authentication failed` or `no password was provided` | Check the password file line (host, port, user, password) and its `chmod 600`, or `PGUSER`. |
| `permission denied to create database` | Use a user that may create databases, such as `postgres`. |
| `No suitable driver found` | Run the tests with the test command from `starter/`; the PostgreSQL driver comes from `pom.xml`. |
| Maven cannot resolve a dependency offline | Run the setup step once with the internet on. |
| You pass 40 minutes | Pause, note the next failing task, and finish it in self-study. |

## Tested on

The shipped starter and a completed set of files were run from a fresh clone of this lab on macOS (arm64) with PostgreSQL 18.3, JDK 25 and Maven 3.9.9 against a local server with trust authentication; the course authors keep that run's record with the course's own evidence. Linux and Windows runs have not yet been recorded; the steps follow PostgreSQL's and pgJDBC's documented behaviour on those systems.
