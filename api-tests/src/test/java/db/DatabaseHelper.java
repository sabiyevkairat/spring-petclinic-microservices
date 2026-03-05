package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC helper for database validation in API tests.
 *
 * Connects directly to the MySQL container to verify that API operations
 * have persisted data correctly. Used exclusively for SELECT assertions —
 * all test data is created through the API, never via direct SQL inserts.
 *
 * Connection defaults (overridable via environment variables):
 *   DB_HOST     → localhost
 *   DB_PORT     → 3306
 *   DB_NAME     → petclinic
 *   DB_USER     → root
 *   DB_PASSWORD → petclinic
 *
 * Usage:
 *   DatabaseHelper db = new DatabaseHelper();
 *   Map<String, Object> row = db.findOwnerById(createdId);
 *   assertThat(row.get("last_name")).isEqualTo("Franklin");
 *   db.close();
 */
public final class DatabaseHelper implements AutoCloseable {

    private final Connection connection;

    public DatabaseHelper() throws SQLException {
        String host     = System.getenv().getOrDefault("DB_HOST",     "localhost");
        String port     = System.getenv().getOrDefault("DB_PORT",     "3306");
        String dbName   = System.getenv().getOrDefault("DB_NAME",     "petclinic");
        String user     = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        String url = String.format(
            "jdbc:mysql://%s:%s/%s?allowPublicKeyRetrieval=true&useSSL=false",
            host, port, dbName
        );

        this.connection = DriverManager.getConnection(url, user, password);
    }

    // ── Owners ────────────────────────────────────────────────────────────────

    /**
     * Returns the owners table row for the given ID, or null if not found.
     */
    public Map<String, Object> findOwnerById(int id) throws SQLException {
        return querySingleRow("SELECT * FROM owners WHERE id = ?", id);
    }

    /**
     * Returns the number of rows in the owners table.
     */
    public int countOwners() throws SQLException {
        return countRows("owners");
    }

    // ── Pets ──────────────────────────────────────────────────────────────────

    /**
     * Returns the pets table row for the given pet ID, or null if not found.
     */
    public Map<String, Object> findPetById(int id) throws SQLException {
        return querySingleRow("SELECT * FROM pets WHERE id = ?", id);
    }

    /**
     * Returns all pets belonging to the given owner ID.
     */
    public List<Map<String, Object>> findPetsByOwnerId(int ownerId) throws SQLException {
        return queryRows("SELECT * FROM pets WHERE owner_id = ?", ownerId);
    }

    // ── Visits ────────────────────────────────────────────────────────────────

    /**
     * Returns the visits table row for the given visit ID, or null if not found.
     */
    public Map<String, Object> findVisitById(int id) throws SQLException {
        return querySingleRow("SELECT * FROM visits WHERE id = ?", id);
    }

    /**
     * Returns all visits for the given pet ID.
     */
    public List<Map<String, Object>> findVisitsByPetId(int petId) throws SQLException {
        return queryRows("SELECT * FROM visits WHERE pet_id = ?", petId);
    }

    // ── Generic helpers ───────────────────────────────────────────────────────

    /**
     * Returns the row count for the given table.
     */
    public int countRows(String table) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT COUNT(*) FROM " + table)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    // ── Internal query helpers ────────────────────────────────────────────────

    private Map<String, Object> querySingleRow(String sql, Object... params)
        throws SQLException {
        List<Map<String, Object>> rows = queryRows(sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, Object>> queryRows(String sql, Object... params)
        throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnName(i).toLowerCase(), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        }
        return results;
    }
}
