package net.sourceforge.mayfly;

import net.sourceforge.mayfly.datastore.*;
import net.sourceforge.mayfly.jdbc.*;
import net.sourceforge.mayfly.ldbc.*;

import java.sql.*;
import java.util.*;

public class Database {

    private DataStore dataStore;

    public Database(DataStore store) {
        dataStore = store;
    }

    public Database() {
        this(new DataStore());
    }

    /**
     * Execute an SQL command which does not return results.
     * This is similar to the JDBC {@link java.sql.Statement#executeUpdate(java.lang.String)}
     * but is more convenient if you have a Database instance around.
     * @return Number of rows changed.
     */
    public int execute(String command) throws SQLException {
        return execute(command, Collections.EMPTY_LIST);
    }

    /**
     * Execute an SQL command which does not return results.
     * This is similar to the JDBC {@link PreparedStatement#executeUpdate()}
     * but might be more convenient if you have a Database instance around.
     * @param sql SQL command, with ? in place of values to be substituted.
     * @param jdbcParameters Values to substitute for the parameters.
     * @return Number of rows changed.
     */
    public int execute(String sql, List jdbcParameters) throws SQLException {
        try {
            Command command = Command.fromTree(Tree.parse(sql));
            command.substitute(jdbcParameters);
            dataStore = command.update(dataStore);
            return command.rowsAffected();
        } catch (MayflyException e) {
            throw e.asSqlException();
        }
    }

    /**
     * Execute an SQL command which returns results.
     * This is similar to the JDBC {@link java.sql.Statement#executeQuery(java.lang.String)}
     * but is more convenient if you have a Database instance around.
     */
    public ResultSet query(String command) throws SQLException {
        Select select = Select.selectFromTree(Tree.parse(command));
        return select.select(dataStore);
    }
    
    /**
     * Only intended for use within Mayfly.
     */
    public ResultSet query(Select select) throws SQLException {
        return select.select(dataStore);
    }

    /**
     * Return table names.
     * 
     * If this functionality is implemented in
     * {@link java.sql.DatabaseMetaData}, this method may go away or become
     * some kind of convenience method.
     */
    public Set tables() {
        return dataStore.tables();
    }

    /**
     * Column names in given table.
     * 
     * If this functionality is implemented in
     * {@link java.sql.DatabaseMetaData}, this method may go away or become
     * some kind of convenience method.
     */
    public List columnNames(String tableName) throws SQLException {
        TableData tableData = dataStore.table(tableName);
        return tableData.columnNames();
    }

    /**
     * Number of rows in given table.
     * 
     * This is a convenience method.  Your production code will almost
     * surely be counting rows (if it needs to at all) via
     * {@link ResultSet} (or the SQL COUNT, once Mayfly implements it).
     * But this method may be convenient in tests.
     */
    public int rowCount(String tableName) throws SQLException {
        TableData tableData = dataStore.table(tableName);
        return tableData.rowCount();
    }

    /**
     * Open a JDBC connection.
     * This is similar to the JDBC {@link DriverManager#getConnection(java.lang.String)}
     * but is more convenient if you have a Database instance around.
     */
    public Connection openConnection() throws SQLException {
        return new JdbcConnection(this);
    }

    /**
     * Take a snapshot of this database.  Specifically, return the data store, which is
     * an immutable object containing all the data, and table definitions, for this
     * database.  Because the data store is immutable, one might store it in a constant
     * and use it from multiple tests.  Here's an example:
     * 
     * <pre>
    static final DataStore standardSetup = makeData();

    private static DataStore makeData() {
        try {
            Database original = new Database();
            original.execute("create table foo (a integer)");
            original.execute("insert into foo(a) values(6)");
            return original.dataStore();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Database database;
    public void setUp() {
        database = new Database(standardSetup);
    }
    </pre>
     */
    public DataStore dataStore() {
        return dataStore;
    }

}
