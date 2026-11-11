package registrar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Public tests for the reporting lab. Every test starts from fresh tables holding the spring term
 * data (../fixtures/term-data.sql) in this run's own lab database on your local server.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class ReportingLabTest {
    private static Connection db;

    @BeforeAll
    static void createDatabase() throws Exception {
        db = LabDatabase.create();
    }

    @AfterAll
    static void dropDatabase() throws Exception {
        LabDatabase.drop(db);
    }

    private static List<String> row(String... values) {
        return List.of(values);
    }

    private static String sql(String task, String file) throws Exception {
        String text = LabDatabase.learnerSql(file);
        if (text == null) {
            fail(task + ": " + file + " has no SQL yet. Write it below the TODO line (README.md, " + task + ").");
        }
        return text;
    }

    private static List<List<String>> report(String task, String file, List<String> columns) throws Exception {
        String text = sql(task, file);
        try {
            List<String> actual = LabDatabase.columns(db, text);
            assertEquals(columns, actual, task + ": " + file + " must return the columns " + columns
                    + " in that order (name them with AS); it returned " + actual + ".");
            return LabDatabase.rows(db, text);
        } catch (SQLException e) {
            String unfinished = String.valueOf(e.getMessage()).contains("syntax error at end of input")
                    ? " The query stops too early: finish it below the supplied part (README.md, " + task + ")." : "";
            fail(task + ": " + file + " did not run. PostgreSQL said: " + e.getMessage() + unfinished);
            return null;
        }
    }

    private static String firstKeyword(String text) {
        String code = text.replaceAll("(?s)/\\*.*?\\*/", " ");
        for (String line : code.split("\\R")) {
            int comment = line.indexOf("--");
            String trimmed = (comment >= 0 ? line.substring(0, comment) : line).strip();
            if (!trimmed.isEmpty()) {
                return trimmed.split("\\s+")[0].toUpperCase(Locale.ROOT);
            }
        }
        return "";
    }

    @Test
    void task1Waitlist() throws Exception {
        LabDatabase.termData(db);
        List<List<String>> actual = report("Task 1", "sql/waitlist.sql", List.of("id", "name", "sql101_grade"));
        List<List<String>> expected = List.of(row("3", "Alan Turing", "88"), row("6", "Barbara Liskov", "88"));
        String hint = "";
        if (actual.stream().anyMatch(r -> r.get(0).equals("1"))) {
            hint = "Ada (student 1) is already enrolled in SQL201, so she is not waitlisted: leave out everyone in SQL201. ";
        } else if (actual.stream().anyMatch(r -> r.get(0).equals("2") || r.get(0).equals("8"))) {
            hint = "Keep only students whose SQL101 grade is at least the SQL101 average (80.6): compare with a scalar subquery. ";
        }
        assertEquals(expected, actual, "Task 1: sql/waitlist.sql returned the wrong rows or order. " + hint
                + "Expected " + expected + " but got " + actual + ".");
        assertEquals("WITH", firstKeyword(sql("Task 1", "sql/waitlist.sql")), "Task 1 (static check of the file text): the rows "
                + "are right; now write the query as a WITH query with named steps, as the task asks.");
    }

    @Test
    void task2TopStudents() throws Exception {
        LabDatabase.termData(db);
        List<List<String>> actual = report("Task 2", "sql/top_students.sql",
                List.of("course_code", "place", "name", "grade", "course_average"));
        List<List<String>> expected = List.of(
                row("JDB301", "1", "Barbara Liskov", "90", "83.5"),
                row("JDB301", "2", "Alan Turing", "77", "83.5"),
                row("PGA110", "1", "Donald Knuth", "65", "65"),
                row("SQL101", "1", "Ada Lovelace", "92", "80.6"),
                row("SQL101", "2", "Alan Turing", "88", "80.6"),
                row("SQL201", "1", "Edsger Dijkstra", "95", "88"),
                row("SQL201", "2", "Ada Lovelace", "81", "88"));
        String hint = "";
        long sql101 = actual.stream().filter(r -> r.get(0).equals("SQL101")).count();
        if (sql101 > 2) {
            hint = "SQL101 has more than two rows: Alan and Barbara tie on 88, and RANK or DENSE_RANK keep both; ROW_NUMBER "
                    + "with student id as the second sort key gives exactly two. ";
        } else if (actual.stream().anyMatch(r -> r.get(3).equals("NULL"))) {
            hint = "Leave out ungraded enrollments before numbering the rows. ";
        } else if (actual.size() == 2) {
            hint = "Only two rows in all: number the rows within each course with PARTITION BY course_code. ";
        }
        assertEquals(expected, actual, "Task 2: sql/top_students.sql returned the wrong rows or order. " + hint
                + "Expected " + expected + " but got " + actual + ".");
    }

    private static final String DASHBOARD =
            "SELECT student_id, grade FROM enrollments WHERE course_code = 'SQL101' ORDER BY grade DESC";

    @Test
    void task3ReportIndex() throws Exception {
        String text = sql("Task 3", "sql/report_index.sql");
        LabDatabase.termData(db);
        try (Statement s = db.createStatement()) {
            s.execute(text);
        } catch (SQLException e) {
            fail("Task 3: sql/report_index.sql did not run. PostgreSQL said: " + e.getMessage());
        }
        List<List<String>> indexes = LabDatabase.rows(db,
                "SELECT i.relname, string_agg(a.attname, ',' ORDER BY k.ord) "
                        + "FROM pg_index AS x "
                        + "JOIN pg_class AS i ON i.oid = x.indexrelid "
                        + "JOIN pg_class AS t ON t.oid = x.indrelid "
                        + "CROSS JOIN LATERAL unnest(x.indkey) WITH ORDINALITY AS k(attnum, ord) "
                        + "JOIN pg_attribute AS a ON a.attrelid = t.oid AND a.attnum = k.attnum "
                        + "WHERE t.relname = 'enrollments' AND NOT x.indisprimary AND k.ord <= x.indnkeyatts "
                        + "GROUP BY i.relname ORDER BY i.relname");
        if (indexes.isEmpty()) {
            fail("Task 3: no new index on enrollments. The primary key's index leads with student_id: PostgreSQL 18 may skip "
                    + "through it to reach one course, but it cannot return that course's rows in grade order, so the plan still "
                    + "needs a Sort. An index on (course_code, grade) does both: write CREATE INDEX ... ON enrollments (...).");
        }
        String name = null;
        for (List<String> index : indexes) {
            if (index.get(1).startsWith("course_code,grade")) {
                name = index.get(0);
            }
        }
        if (name == null) {
            fail("Task 3: your index on enrollments has the key columns " + indexes.get(0).get(1) + ". The dashboard filters on "
                    + "course_code and then sorts by grade, so a multicolumn index with course_code first and grade second "
                    + "serves both.");
        }
        List<String> plan = new ArrayList<>();
        try (Statement s = db.createStatement()) {
            s.execute("ANALYZE enrollments");
            s.execute("SET enable_seqscan = off");
            try (var r = s.executeQuery("EXPLAIN " + DASHBOARD)) {
                while (r.next()) {
                    plan.add(r.getString(1).strip());
                }
            }
            s.execute("RESET enable_seqscan");
        }
        String index = name;
        boolean usesIndex = plan.stream().anyMatch(line -> line.matches("^(->\\s+)?[A-Za-z ]*Index[A-Za-z ]* (using|on) \"?"
                + java.util.regex.Pattern.quote(index) + "\"?( .*)?$"));
        boolean sorts = plan.stream().anyMatch(line -> line.matches("^(->\\s+)?(Incremental )?Sort\\b.*"));
        assertTrue(usesIndex && !sorts, "Task 3: the dashboard query's plan does not read the rows in grade order from your "
                + "index " + name + ". EXPLAIN shows: " + String.join(" | ", plan));
    }
}
