package registrar;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;

/**
 * Creates a new, uniquely named lab database on your local PostgreSQL server, gives every test a
 * fresh copy of the registrar tables, and drops that database again when the tests finish.
 *
 * Safety: each run invents a fresh name such as reporting_lab_3f9c0a1b7d2e and creates it. If a
 * database with that name already exists, the run picks another name; it never drops a database it
 * did not create. While the run owns a database, its name is written to target/lab-database.txt,
 * so an interrupted run can be cleaned up by name.
 *
 * Connection: pgJDBC and psql both use the variables psql reads: PGHOST (a host name or IP
 * address, default localhost), PGPORT (default 5432), PGUSER (default: your login name) and
 * PGDATABASE (the existing database used to run CREATE DATABASE and DROP DATABASE, default
 * postgres). The password comes from your password file, or PGPASSWORD.
 */
final class LabDatabase {
    static final String PREFIX = "reporting_lab_";
    static final Path RECORD = Path.of("target", "lab-database.txt");
    static final Path FIXTURES = Path.of("..", "fixtures");

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DUPLICATE_DATABASE = "42P04";

    private static String owned;

    private LabDatabase() {
    }

    static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    static String host() throws SQLException {
        String host = env("PGHOST", "localhost");
        if (host.startsWith("/") || host.contains("\\") || host.contains(",")) {
            throw new SQLException("PGHOST is set to \"" + host + "\", which is not a host name. "
                    + "The tests connect over TCP, so set PGHOST to a host name or IP address such as localhost "
                    + "(see README.md, Setup).");
        }
        return host;
    }

    static String user() {
        return env("PGUSER", System.getProperty("user.name"));
    }

    private static Connection connect(String database) throws SQLException {
        String h = host();
        String url = "jdbc:postgresql://" + (h.contains(":") ? "[" + h + "]" : h) + ":" + env("PGPORT", "5432") + "/" + database;
        Properties props = new Properties();
        props.setProperty("user", user());
        String password = System.getenv("PGPASSWORD");
        if (password != null && !password.isEmpty()) {
            props.setProperty("password", password);
        }
        return DriverManager.getConnection(url, props);
    }

    private static void admin(String sql) throws SQLException {
        try (Connection c = connect(env("PGDATABASE", "postgres")); Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    /** Creates a new lab database that this run owns and opens a connection to it. */
    static Connection create() throws SQLException, IOException {
        String name = null;
        for (int attempt = 0; attempt < 5 && name == null; attempt++) {
            byte[] bytes = new byte[6];
            RANDOM.nextBytes(bytes);
            String candidate = PREFIX + HexFormat.of().formatHex(bytes);
            try {
                admin("CREATE DATABASE " + candidate);
                name = candidate;
            } catch (SQLException e) {
                if (DUPLICATE_DATABASE.equals(e.getSQLState())) {
                    continue; // someone else's database has this name: leave it alone and try another
                }
                throw new SQLException("Could not reach PostgreSQL to create the lab database. "
                        + "Check that the server is running and that the connection check in README.md "
                        + "(Prerequisites) works with the same settings. Server said: " + e.getMessage(), e);
            }
        }
        if (name == null) {
            throw new SQLException("Could not find an unused name for the lab database; run the tests again.");
        }
        owned = name;
        Files.createDirectories(RECORD.getParent());
        Files.writeString(RECORD, name + System.lineSeparator());
        return connect(name);
    }

    static String ownedName() {
        return owned;
    }

    /** Closes the connection and drops only the database this run created. */
    static void drop(Connection c) throws SQLException, IOException {
        if (c != null) {
            c.close();
        }
        if (owned != null) {
            // FORCE also ends connections your repository code may have left open (PostgreSQL 13 and later).
            admin("DROP DATABASE IF EXISTS " + owned + " WITH (FORCE)");
            owned = null;
            Files.deleteIfExists(RECORD);
        }
    }

    /** Replaces every table with empty ones from ../fixtures/schema.sql. */
    static void emptyTables(Connection c) throws SQLException, IOException {
        try (Statement s = c.createStatement()) {
            s.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
            s.execute(Files.readString(FIXTURES.resolve("schema.sql")));
        }
    }

    /** Empty tables plus the spring term data from ../fixtures/term-data.sql. */
    static void termData(Connection c) throws SQLException, IOException {
        emptyTables(c);
        try (Statement s = c.createStatement()) {
            s.execute(Files.readString(FIXTURES.resolve("term-data.sql")));
        }
    }

    /** The learner's file, or null when it still holds only comments and blank lines. */
    static String learnerSql(String file) throws IOException {
        String text = Files.readString(Path.of(file));
        StringBuilder code = new StringBuilder();
        for (String line : text.split("\\R")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                code.append(line).append('\n');
            }
        }
        return code.isEmpty() ? null : text;
    }

    /** Runs one SELECT and returns its rows as text, with NULL written as "NULL". */
    static List<List<String>> rows(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            return rows(r);
        }
    }

    static List<List<String>> rows(ResultSet r) throws SQLException {
        ResultSetMetaData meta = r.getMetaData();
        List<List<String>> out = new ArrayList<>();
        while (r.next()) {
            List<String> row = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                Object value = r.getObject(i);
                if (value == null) {
                    row.add("NULL");
                } else if (value instanceof BigDecimal d) {
                    row.add(d.signum() == 0 ? "0" : d.stripTrailingZeros().toPlainString());
                } else {
                    row.add(value.toString());
                }
            }
            out.add(row);
        }
        return out;
    }

    static List<String> columns(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            ResultSetMetaData meta = r.getMetaData();
            List<String> names = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                names.add(meta.getColumnLabel(i));
            }
            return names;
        }
    }

}
