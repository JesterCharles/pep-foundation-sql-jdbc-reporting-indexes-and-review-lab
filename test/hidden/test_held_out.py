"""Held-out tests: graded on submit, never shown to the learner."""

import re

from test.support import DASHBOARD, create_report_index, first_keyword, term_data, waitlist_rows


def test_task1_waitlist_is_a_with_query(db):
    term_data(db)
    waitlist_rows(db)
    assert first_keyword("waitlist.sql") == "WITH", (
        "Task 1 (static check of the file text): write the query as a WITH query with named steps, as the task asks.")


def test_task3_plan_reads_the_index_in_grade_order(db):
    name = create_report_index(db)
    db.execute("ANALYZE enrollments")
    db.execute("SET enable_seqscan = off")
    plan = [row[0].strip() for row in db.execute("EXPLAIN " + DASHBOARD).fetchall()]
    db.execute("RESET enable_seqscan")
    uses_index = any(re.match(r"^(->\s+)?[A-Za-z ]*Index[A-Za-z ]* (using|on) \"?" + re.escape(name) + r"\"?( .*)?$", line)
                     for line in plan)
    sorts = any(re.match(r"^(->\s+)?(Incremental )?Sort\b.*", line) for line in plan)
    assert uses_index and not sorts, ("Task 3: the dashboard query's plan does not read the rows in grade order from your "
                                      f"index {name}. EXPLAIN shows: {' | '.join(plan)}")
