# Reporting lab: the registrar's term reports

The spring term is under way in the registrar database. The course coordinator needs a waitlist, a top-students report and a fast course dashboard.
You finish three SQL files. The tests run each one against a fresh PostgreSQL database.

You practise WITH queries with named steps, window functions and multicolumn indexes read through `EXPLAIN`.

**Time:** about 20 minutes, at most 40 in one sitting. If you pass 40 minutes, note the next failing task and finish it later.

You need no database server of your own: the tests start a private PostgreSQL 16 for each run and delete it afterwards.

## Quick start

1. Open the lab project. The files you edit are in `src/`; the checklist below names them.
2. Select **Run Tests** before you change anything.

   **All 3 visible tests fail at first. That is expected: each failure is a task you have not done yet.**
   Every failure message starts with its task number, such as `Task 1:`.
3. Work through the checklist below. After each task, select **Run Tests** again and read the remaining `Task N:` messages.
4. **You are finished when all 3 visible tests pass.** Then select **Submit**.
   Submitting also runs held-out tests you can't see. They check that each file does what its task asks in full, including how it is written where the task says so.

```
OPEN project -> RUN TESTS: 3 fail (expected)
  -> READ the "Task N:" messages -> EDIT the next TODO -> RUN TESTS again
  -> repeat for Tasks 1-3 -> SUBMIT when all 3 tests pass
```

In words: open the project, run the tests, read which tasks fail, complete one task's TODO, run the tests again, and submit when they all pass.

## Checklist

You edit only the three files below, all in `src/`. Each one has a `TODO` comment that marks where your work goes. You can do the tasks in any order.

- [ ] Task 1: SQL201 waitlist, `waitlist.sql` (test `test_task1_waitlist`)
- [ ] Task 2: top two per course, `top_students.sql` (test `test_task2_top_students`)
- [ ] Task 3: dashboard index, `report_index.sql` (test `test_task3_report_index`)

A task is done when no failure message starts with its number.
The tests and their data in `test/` are restored to their original state before your work is graded, so changes to them do not count.

From a terminal in the project folder, you can run one task's test on its own, for example `python -m pytest -q -k task1`, or all of them with `python -m pytest -q`.

## The data

`test/fixtures/schema.sql` creates the registrar tables, and `test/fixtures/term-data.sql` holds the spring term: 4 courses, 8 students and 12 enrollments, some still ungraded (NULL). Before every test, the tests rebuild the tables from these two files.

**Try it by hand (optional, not tested).** In a terminal, from the project folder:

```
python -m test.practice
```

It starts a private PostgreSQL with the spring term data and opens a `psql` prompt. Try your queries there; `\d enrollments` lists the indexes PostgreSQL already made for the primary key.
`python -m test.practice src/waitlist.sql` runs your file once before the prompt opens.
Leave with `\q`: the practice database is deleted, so the next start is fresh.

## Task steps

### Task 1: SQL201 waitlist (`waitlist.sql`)

Offer a SQL201 place to every student whose SQL101 grade is at least the SQL101 average grade and who is not already enrolled in SQL201.
- The file supplies the first named step, `sql101_average`, which computes the average.
- Add a second step, `sql201_students`, that lists who is already in SQL201, then the final `SELECT`.
- Columns `id`, `name`, `sql101_grade`, sorted by `sql101_grade` from highest to lowest, then `id`.
- Keep it one `WITH` query: the file's statement must start with `WITH`.

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
- The tests check that your index has the right key columns, and that `EXPLAIN` on the dashboard query reads the rows from your index in grade order, with no separate Sort step. They first turn sequential scans off (`SET enable_seqscan = off`), because on 12 rows PostgreSQL would rightly prefer reading the whole small table.

<details><summary>Hint</summary>

Use a multicolumn B-tree index whose columns match the query: the filtered column first, then the sorted one. In the practice database, run `SET enable_seqscan = off;` and then `EXPLAIN` the same query before and after creating the index to see the change.

</details>

**Starting over (optional):** before you change a file, keep a copy of it outside the project folder. To restart that file, paste the copy back.

## What the tests do

The first test starts a private PostgreSQL 16 from the `pgserver` package in a temporary folder.
Every test then gets its own new database with the spring term tables, runs your file in it, checks the result, and drops that database.
At the end of the run the server and its folder are deleted. The tests never connect to any other database.

## Using an AI assistant

If you use an AI assistant, ask it to explain, not to answer.
"Explain what PARTITION BY does in a window function" teaches you SQL; "write my report" does not.
Check any explanation with a test run before you trust it.

## Troubleshooting

| What you see | What to do |
| --- | --- |
| `pgserver package, which is not installed` | Run `python -m pip install -r requirements.txt`. `pgserver` needs Python 3.9 to 3.12; **Run Tests** uses a supported Python. |
| `syntax error at end of input` | The query stops too early: finish it below the supplied part. |
| `must return the columns ... in that order` | Name each column with `AS` exactly as the task lists it, in that order. |
