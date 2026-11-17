# Reporting lab: the registrar's term reports

The spring term is under way in the registrar database. The course coordinator needs a waitlist, a top-students report and a fast course dashboard.
You finish three SQL files. The tests run each one against a fresh database on the PostgreSQL server the tests connect to.

You practise WITH queries with named steps, window functions and multicolumn indexes read through `EXPLAIN`.

**Time:** about 20 minutes, at most 40 in one sitting. If you pass 40 minutes, note the next failing task and finish it later.

## Before you start: PostgreSQL

The tests need a PostgreSQL server they can reach (16 or later). See [Prerequisites](#prerequisites) for the connection check, and [Setup](#setup) if your server needs other connection settings or a password.
If no server is reachable, every test fails with `Could not reach PostgreSQL to create the lab database`. That is a missing prerequisite, not a mistake in your SQL.

## Quick start

1. Open the lab project. The files you edit are in `src/main/sql/`; the checklist below names them.
2. Select **Run Tests** before you change anything.

   **All 3 tests fail at first. That is expected: each failure is a task you have not done yet.**
   Every failure message starts with its task number, such as `Task 1:`.
3. Work through the checklist below. After each task, select **Run Tests** again and read the remaining `Task N:` messages.
4. **You are finished when all 3 tests pass.** Then select **Submit**.

```
OPEN project -> RUN TESTS: 3 fail (expected)
  -> READ the "Task N:" messages -> EDIT the next TODO -> RUN TESTS again
  -> repeat for Tasks 1-3 -> SUBMIT when all 3 tests pass
```

In words: open the project, run the tests, read which tasks fail, complete one task's TODO, run the tests again, and submit when they all pass.

## Checklist

You edit only the three files below, all in `src/main/sql/`. Each one has a `TODO` comment that marks where your work goes. You can do the tasks in any order.

- [ ] Task 1: SQL201 waitlist, `waitlist.sql` (test `task1Waitlist`)
- [ ] Task 2: top two per course, `top_students.sql` (test `task2TopStudents`)
- [ ] Task 3: dashboard index, `report_index.sql` (test `task3ReportIndex`)

A task is done when no failure message starts with its number.
The tests in `src/test/java/registrar/` are restored to their original state before your work is graded, so changes to them do not count. Leave the build file `pom.xml`, the `fixtures/` folder and `expected/test-summary.txt` as they are.

From a terminal in the project folder, you can run one task's test on its own, for example `mvn -q test -Dtest='ReportingLabTest#task1Waitlist'`, or all of them with `mvn -q test`.
`WARNING:` lines from Java or Maven, such as `WARNING: A restricted method ...`, are not test failures.

## The data

`fixtures/schema.sql` creates the registrar tables, and `fixtures/term-data.sql` holds the spring term: 4 courses, 8 students and 12 enrollments, some still ungraded (NULL). Before every test, the tests rebuild the tables from these two files.

**Try it in psql (optional, not tested).** In a terminal, from the project folder:

```
psql -h localhost -d postgres -c "CREATE DATABASE reporting_scratch;"
psql -h localhost -d reporting_scratch -f fixtures/schema.sql
psql -h localhost -d reporting_scratch -f fixtures/term-data.sql
psql -h localhost -d reporting_scratch
```

Try your queries at the prompt; `\d enrollments` lists the indexes PostgreSQL already made for the primary key, and `\q` leaves. When you finish, drop it with `psql -h localhost -d postgres -c "DROP DATABASE reporting_scratch;"`.

## Task steps

### Task 1: SQL201 waitlist (`waitlist.sql`)

Offer a SQL201 place to every student whose SQL101 grade is at least the SQL101 average grade and who is not already enrolled in SQL201.
- The file supplies the first named step, `sql101_average`, which computes the average.
- Add a second step, `sql201_students`, that lists who is already in SQL201, then the final `SELECT`.
- Columns `id`, `name`, `sql101_grade`, sorted by `sql101_grade` from highest to lowest, then `id`.
- The test also checks that the file's statement starts with `WITH`.

<details><summary>Hint</summary>

In the final `SELECT`, compare each grade with the average through a scalar subquery, and leave out the SQL201 students with `NOT IN` or `NOT EXISTS`.

</details>

### Task 2: top two per course (`top_students.sql`)

For every course, the two best graded students. The file supplies a `WITH` step, `ranked`, that already selects each graded enrollment with its student's name.
- Where the TODO comment is, add a `place` column that numbers each course's rows by grade, highest first. Student id breaks ties, so Alan and Barbara, who both have 88 in SQL101, always come out in the same order.
- Add the course's average grade, `course_average`, on every row.
- Below the step, write the outer query that keeps places 1 and 2.
- Columns `course_code`, `place`, `name`, `grade`, `course_average`, sorted by `course_code`, then `place`.

<details><summary>Hint</summary>

Number the rows with `ROW_NUMBER() OVER (PARTITION BY course_code ORDER BY grade DESC, student_id)`. Add the average with `AVG(grade) OVER (PARTITION BY course_code)`, which keeps each row instead of collapsing the group. With `RANK` or `DENSE_RANK` and no tie-breaker, tied students would share a place and a course could show three rows.

</details>

### Task 3: dashboard index (`report_index.sql`)

The dashboard runs `SELECT student_id, grade FROM enrollments WHERE course_code = ? ORDER BY grade DESC`. The primary key's index leads with `student_id`, so it cannot hand back one course's rows in grade order, and the plan still needs a Sort step.
- Write one `CREATE INDEX`, with a name of your choice, so the plan reads one course's rows in grade order.
- The test runs `EXPLAIN` on the dashboard query and checks that the plan reads the rows from your index in grade order, with no separate Sort step. It first turns sequential scans off (`SET enable_seqscan = off`), because on 12 rows PostgreSQL would rightly prefer reading the whole small table. If the test fails, it prints the plan: read which scan it chose.

<details><summary>Hint</summary>

Use a multicolumn B-tree index whose columns match the query: the filtered column first, then the sorted one. In your scratch database, `EXPLAIN` the same query before and after creating the index to see the change.

</details>

**Starting over (optional):** before you change a file, keep a copy of it outside the project folder. To restart that file, paste the copy back.

## What the tests do

Each run creates its own database with a unique name such as `reporting_lab_3f9c0a1b7d2e`, runs the tests in it, and drops it.
It never touches a database it did not create. While a run is in progress, the database's name is in `target/lab-database.txt`.
The tests talk only to that PostgreSQL server.

## Using an AI assistant

If you use an AI assistant, ask it to explain, not to answer.
"Explain what PARTITION BY does in a window function" teaches you SQL; "write my report" does not.
Check any explanation with a test run before you trust it.

## Prerequisites

| Tool | Version | Check |
| --- | --- | --- |
| PostgreSQL server and `psql` | 16 or later | the connection check below |
| JDK (only for terminal runs) | 17 or later | `java -version` |
| Apache Maven (only for terminal runs) | 3.9 or later | `mvn -v` |

### Connection check

```
psql -h localhost -p 5432 -d postgres -c "SELECT version();"
```

It should print one row starting `PostgreSQL 16` (or later) without asking for a password.

## Setup

Only if the connection check needs other values: the tests read the same environment variables as `psql`.

| Variable | Meaning | Default |
| --- | --- | --- |
| `PGHOST` | server host name | `localhost` |
| `PGPORT` | server port | `5432` |
| `PGUSER` | a PostgreSQL user allowed to create databases | your login name |
| `PGDATABASE` | an existing database to connect to first | `postgres` |

For a terminal run, set the ones you need in that terminal, for example `export PGUSER=postgres` (macOS or Linux) or `$env:PGUSER = "postgres"` (Windows PowerShell). They last until you close it, so set them again in a new terminal.

**If your server asks for a password**, put one line, `localhost:5432:*:postgres:your-password`, in `~/.pgpass` and run `chmod 600 ~/.pgpass` (macOS or Linux), or in `%APPDATA%\postgresql\pgpass.conf` (Windows). Never write a password into a lab file.

## Troubleshooting

| What you see | What to do |
| --- | --- |
| `Could not reach PostgreSQL to create the lab database` | Run the [connection check](#connection-check) until it prints the version. |
| `password authentication failed` or `no password was provided` | Set up a [password file](#setup), or set `PGUSER`. |
| `permission denied to create database` | Use a PostgreSQL user that may create databases, such as `postgres`. |
| `No suitable driver found` | Run the tests with **Run Tests** or from the project folder; the PostgreSQL driver comes from `pom.xml`. |
| A run was interrupted | Drop the database named in `target/lab-database.txt`, for example `psql -h localhost -d postgres -c "DROP DATABASE reporting_lab_3f9c0a1b7d2e WITH (FORCE);"` with the name from the file. |
