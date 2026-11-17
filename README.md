# Reporting lab: the registrar's term reports

The spring term is under way in the registrar database. The course coordinator needs a waitlist, a top-students report and a fast course dashboard.
You finish three SQL files, and the tests run each one against a fresh database on a PostgreSQL server.

You practise WITH queries with named steps, window functions and multicolumn indexes read through `EXPLAIN`.

The full learner instructions are in [`.evolv/instructions.md`](.evolv/instructions.md), which the lab platform shows as the lab's instructions.

## Layout

This repository follows the Exa coding-lab scaffold for Java.

| Path | What it is |
|---|---|
| `src/main/sql/waitlist.sql` | Lab source: Task 1 |
| `src/main/sql/top_students.sql` | Lab source: Task 2 |
| `src/main/sql/report_index.sql` | Lab source: Task 3 |
| `src/test/java/registrar/ReportingLabTest.java` | Visible tests |
| `src/test/java/registrar/LabDatabase.java` | Test support: creates and drops the run's own lab database |
| `fixtures/schema.sql`, `fixtures/term-data.sql` | The registrar tables and the spring term data each test starts from |
| `.evolv/instructions.md` | Learner instructions |
| `.evolv/grade.yml` | Grading manifest, used by Run Tests and by the grader |
| `pom.xml` | Maven build: Java 17 and JUnit 5.11.3, matching the scaffold, plus the PostgreSQL JDBC driver |
| `Dockerfile` | Image for local Run Tests; warms the Maven cache |
| `expected/test-summary.txt` | The result of a finished lab |

There are no held-out tests in this lab: every test is visible.

## PostgreSQL prerequisite

The tests need a running PostgreSQL server (16 or later) that they reach over TCP.
They read `PGHOST`, `PGPORT`, `PGUSER` and `PGDATABASE` (defaults `localhost`, `5432`, the login user, `postgres`) and a password file or `PGPASSWORD`.
The user must be allowed to create databases: each run creates its own uniquely named database and drops it afterwards.
Without a reachable server every test fails with `Could not reach PostgreSQL to create the lab database`.

## Run the tests

On the platform, select **Run Tests**. From a terminal with JDK 17 or later, Apache Maven 3.9 and a reachable PostgreSQL server:

```
mvn -q test
```

All 3 tests fail until the `TODO`s are done. That is expected.
