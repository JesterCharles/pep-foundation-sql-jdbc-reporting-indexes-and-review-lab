"""Trainer-authored visible tests — restored over the learner checkout at grade time.

Every test starts from fresh tables holding the spring term data (test/fixtures/term-data.sql)
in its own new database.
"""

from test.support import WAITLIST, create_report_index, report, term_data, waitlist_rows


def test_task1_waitlist(db):
    term_data(db)
    actual = waitlist_rows(db)
    hint = ""
    if any(r[0] == "1" for r in actual):
        hint = "Ada (student 1) is already enrolled in SQL201, so she is not waitlisted: leave out everyone in SQL201. "
    elif any(r[0] in ("2", "8") for r in actual):
        hint = ("Keep only students whose SQL101 grade is at least the SQL101 average (80.6): compare with a scalar subquery. ")
    assert actual == WAITLIST, (f"Task 1: waitlist.sql returned the wrong rows or order. {hint}"
                                f"Expected {WAITLIST} but got {actual}.")


def test_task2_top_students(db):
    term_data(db)
    actual = report(db, "Task 2", "top_students.sql", ["course_code", "place", "name", "grade", "course_average"])
    expected = [
        ["JDB301", "1", "Barbara Liskov", "90", "83.5"],
        ["JDB301", "2", "Alan Turing", "77", "83.5"],
        ["PGA110", "1", "Donald Knuth", "65", "65"],
        ["SQL101", "1", "Ada Lovelace", "92", "80.6"],
        ["SQL101", "2", "Alan Turing", "88", "80.6"],
        ["SQL201", "1", "Edsger Dijkstra", "95", "88"],
        ["SQL201", "2", "Ada Lovelace", "81", "88"],
    ]
    hint = ""
    if sum(1 for r in actual if r[0] == "SQL101") > 2:
        hint = ("SQL101 has more than two rows: Alan and Barbara tie on 88, and RANK or DENSE_RANK keep both; ROW_NUMBER "
                "with student id as the second sort key gives exactly two. ")
    elif any(r[3] == "NULL" for r in actual):
        hint = "Leave out ungraded enrollments before numbering the rows. "
    elif len(actual) == 2:
        hint = "Only two rows in all: number the rows within each course with PARTITION BY course_code. "
    assert actual == expected, (f"Task 2: top_students.sql returned the wrong rows or order. {hint}"
                                f"Expected {expected} but got {actual}.")


def test_task3_report_index(db):
    create_report_index(db)
