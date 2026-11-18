"""Test plumbing shared by the visible tests and the held-out tests.

You need no PostgreSQL server of your own. The pgserver package (see requirements.txt) contains a
real PostgreSQL 16. The tests start a private copy of it in a temporary folder, give every test a
brand-new empty database, and delete the server and its data when the run ends. They never touch
any other database.
"""

from __future__ import annotations

import contextlib
import re
import secrets
import tempfile
from decimal import Decimal
from pathlib import Path

import psycopg
import pytest

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src"
FIXTURES = Path(__file__).resolve().parent / "fixtures"


# ---------- the private PostgreSQL ----------

def start_server():
    """Starts a private PostgreSQL in a new temporary folder and returns its pgserver handle."""
    try:
        import pgserver
    except ImportError as error:
        raise RuntimeError(
            "The tests start their own PostgreSQL with the pgserver package, which is not installed. "
            "Run: python -m pip install -r requirements.txt (pgserver needs Python 3.9 to 3.12)."
        ) from error
    return pgserver.get_server(tempfile.mkdtemp(prefix="pglab-"), cleanup_mode="delete")


@contextlib.contextmanager
def fresh_database(server):
    """Creates a new, empty database, yields an autocommit connection to it, then drops it."""
    name = "lab_" + secrets.token_hex(6)
    with psycopg.connect(server.get_uri(), autocommit=True) as admin:
        admin.execute(f"CREATE DATABASE {name}")
        try:
            with psycopg.connect(server.get_uri(database=name), autocommit=True) as conn:
                yield conn
        finally:
            admin.execute(f"DROP DATABASE IF EXISTS {name} WITH (FORCE)")


# ---------- the spring term data ----------

def term_data(db) -> None:
    """Fresh registrar tables holding the spring term, from test/fixtures/schema.sql and term-data.sql."""
    db.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;")
    db.execute((FIXTURES / "schema.sql").read_text(encoding="utf-8"))
    db.execute((FIXTURES / "term-data.sql").read_text(encoding="utf-8"))


# ---------- the learner's files ----------

def learner_sql(task: str, name: str) -> str:
    """The learner's file src/<name>; fails the task while it holds only comments and blank lines."""
    content = (SRC / name).read_text(encoding="utf-8")
    if not any(line.strip() and not line.strip().startswith("--") for line in content.splitlines()):
        pytest.fail(f"{task}: {name} has no SQL yet. Write it below the TODO line (lab README, {task}).", pytrace=False)
    return content


def code_only(name: str) -> str:
    """The file's SQL with block comments, -- comments and blank lines removed, for the few checks that read the text."""
    content = re.sub(r"/\*.*?\*/", " ", (SRC / name).read_text(encoding="utf-8"), flags=re.S)
    kept = [line.split("--", 1)[0].strip() for line in content.splitlines()]
    return "\n".join(line for line in kept if line) + "\n"


# ---------- results as text ----------

def text(value) -> str:
    """A value as the tests compare it: NULL for a missing value, numbers without trailing zeros."""
    if value is None:
        return "NULL"
    if isinstance(value, Decimal):
        return "0" if value == 0 else format(value.normalize(), "f")
    return str(value)


def _rows(cursor) -> list[list[str]]:
    return [[text(value) for value in row] for row in cursor.fetchall()]


def table(db, sql: str) -> list[list[str]]:
    """Runs one SELECT and returns its rows as text, with NULL written as "NULL"."""
    return _rows(db.execute(sql))


def report(db, task: str, name: str, columns: list[str]) -> list[list[str]]:
    """Runs a report file and checks its column names; returns its rows."""
    sql = learner_sql(task, name)
    problem = None
    try:
        cursor = db.execute(sql)
    except psycopg.Error as error:
        problem = str(error).strip()
    if problem is not None:
        unfinished = (f" The query stops too early: finish it below the supplied part (lab README, {task})."
                      if "syntax error at end of input" in problem else "")
        pytest.fail(f"{task}: {name} did not run. PostgreSQL said: {problem}{unfinished}", pytrace=False)
    actual = [column.name for column in cursor.description] if cursor.description else []
    assert actual == columns, (f"{task}: {name} must return the columns {columns} in that order "
                               f"(name them with AS); it returned {actual}.")
    return _rows(cursor)


def first_keyword(name: str) -> str:
    """The first word of the file's SQL, ignoring comments, in upper case."""
    code = code_only(name).split()
    return code[0].upper() if code else ""


WAITLIST = [["3", "Alan Turing", "88"], ["6", "Barbara Liskov", "88"]]


def waitlist_rows(db) -> list[list[str]]:
    return report(db, "Task 1", "waitlist.sql", ["id", "name", "sql101_grade"])


DASHBOARD = "SELECT student_id, grade FROM enrollments WHERE course_code = 'SQL101' ORDER BY grade DESC"


def create_report_index(db) -> str:
    """Task 3: runs report_index.sql and returns the name of the new (course_code, grade) index on enrollments."""
    sql = learner_sql("Task 3", "report_index.sql")
    term_data(db)
    problem = None
    try:
        db.execute(sql)
    except psycopg.Error as error:
        problem = str(error).strip()
    if problem is not None:
        pytest.fail(f"Task 3: report_index.sql did not run. PostgreSQL said: {problem}", pytrace=False)
    indexes = table(db,
                    "SELECT i.relname, string_agg(a.attname, ',' ORDER BY k.ord) "
                    "FROM pg_index AS x "
                    "JOIN pg_class AS i ON i.oid = x.indexrelid "
                    "JOIN pg_class AS t ON t.oid = x.indrelid "
                    "CROSS JOIN LATERAL unnest(x.indkey) WITH ORDINALITY AS k(attnum, ord) "
                    "JOIN pg_attribute AS a ON a.attrelid = t.oid AND a.attnum = k.attnum "
                    "WHERE t.relname = 'enrollments' AND NOT x.indisprimary AND k.ord <= x.indnkeyatts "
                    "GROUP BY i.relname ORDER BY i.relname")
    if not indexes:
        pytest.fail("Task 3: no new index on enrollments. The primary key's index leads with student_id, so it cannot hand "
                    "back one course's rows in grade order, and the plan still needs a Sort. An index on (course_code, grade) "
                    "does both: write CREATE INDEX ... ON enrollments (...).", pytrace=False)
    for index_name, keys in indexes:
        if keys.startswith("course_code,grade"):
            return index_name
    pytest.fail(f"Task 3: your index on enrollments has the key columns {indexes[0][1]}. The dashboard filters on "
                "course_code and then sorts by grade, so a multicolumn index with course_code first and grade second "
                "serves both.", pytrace=False)
