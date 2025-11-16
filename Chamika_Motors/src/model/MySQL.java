package model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import util.DBUtil;

/**
 * Lightweight helper to execute simple SQL statements.
 *
 * @deprecated This class has multiple issues:
 * <ul>
 *   <li>Resource leak risk: Returns open ResultSet for SELECT queries, requiring callers to manually close resources</li>
 *   <li>Security risk: Encourages SQL string concatenation instead of PreparedStatements, leading to SQL injection vulnerabilities</li>
 *   <li>Poor error handling: Uses printStackTrace() instead of proper logging</li>
 * </ul>
 * <p>
 * Instead, use the repository pattern with PreparedStatements and try-with-resources:
 * <ul>
 *   <li>For new code: Create a repository class implementing proper CRUD operations</li>
 *   <li>For existing code: Migrate to use existing repository/service layer implementations</li>
 * </ul>
 * <p>
 * This class will be removed in a future version.
 *
 * @see repository
 * @see service
 */
@Deprecated
public class MySQL {

    /**
     * @deprecated Use repository pattern with PreparedStatements instead
     */
    @Deprecated
    public static ResultSet execute(String query) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DBUtil.getConnection();
            statement = connection.createStatement();

            if (query.trim().toUpperCase().startsWith("SELECT")) {
                // Return the ResultSet (caller must close resources)
                return statement.executeQuery(query);
            } else {
                statement.executeUpdate(query);
                // close resources for non-select
                DBUtil.closeQuietly(statement, connection);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Attempt to close resources on error
            DBUtil.closeQuietly(statement, connection);
            return null;
        }
    }
}
