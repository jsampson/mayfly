package net.sourceforge.mayfly.dump;

import junit.framework.TestCase;

import net.sourceforge.mayfly.Database;
import net.sourceforge.mayfly.MayflyException;
import net.sourceforge.mayfly.datastore.DataStore;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.PreparedStatement;

public class SqlDumperTest extends TestCase {
    
    private Database database;

    protected void setUp() throws Exception {
        database = new Database();
    }

    public void testEmpty() throws Exception {
        assertEquals("", new SqlDumper().dump(new Database().dataStore()));
    }
    
    public void testWriter() throws Exception {
        StringWriter out = new StringWriter();
        new SqlDumper().dump(new DataStore(), out);
        assertEquals("", out.toString());
    }

    public void testTable() throws Exception {
        database.execute("create table foo(a integer)");
        assertEquals("CREATE TABLE foo(\n  a INTEGER\n);\n\n", 
            dump());
    }

    public void testTwoColumns() throws Exception {
        database.execute("create table foo(a integer, B Integer)");
        assertEquals("CREATE TABLE foo(\n" +
                "  a INTEGER,\n" +
                "  B INTEGER\n" +
                ");\n\n", 
            dump());
    }
    
    public void testTwoTables() throws Exception {
        database.execute("create table foo(a integer)");
        database.execute("create table bar(b integer)");
        assertEquals(
            "CREATE TABLE foo(\n" +
            "  a INTEGER\n" +
            ");\n" + 
            "\n" +
            "CREATE TABLE bar(\n" +
            "  b INTEGER\n" +
            ");\n\n", 
            dump());
    }
    
    public void testDataTypes() throws Exception {
        database.execute("create table Foo(" +
            "b varchar ( 0243 ) ," +
            "c timestamp," +
            "d date," +
            "e text," +
            "f decimal ( 7 , 5 ), " +
            "g   blob ( 32800)," +
            "h blob" +
            ")");
        assertEquals(
            "CREATE TABLE Foo(\n" +
            "  b VARCHAR(243),\n" +
            "  c TIMESTAMP,\n" +
            "  d DATE,\n" +
            "  e TEXT,\n" +
            "  f DECIMAL(7,5),\n" +
            "  g BLOB(32800),\n" +
            "  h BLOB\n" +
            ");\n\n", 
            dump()
        );
    }

    public void testIntegerDataTypes() throws Exception {
        database.execute("create table Foo(" +
            "a integer," +
            "b int ," +
            "c tinyint," +
            "d smallint," +
            "e bigint," +
            "f identity," +
            "g serial" +
            ")");
        assertEquals(
            "CREATE TABLE Foo(\n" +
            "  a INTEGER,\n" +
            // The prevailing concept here mostly seems to be to canonicalize.
            "  b INTEGER,\n" +
            "  c TINYINT,\n" +
            "  d SMALLINT,\n" +
            "  e BIGINT,\n" +
            // Probably should be f INTEGER AUTO_INCREMENT NOT NULL or some such
            "  f IDENTITY,\n" +
            "  g IDENTITY\n" +
            ");\n\n", 
            dump()
        );
    }
    
    public void testRow() throws Exception {
        database.execute("create table foo(a integer)");
        database.execute("insert into foo(a) values(5)");
        assertEquals("CREATE TABLE foo(\n  a INTEGER\n);\n\n" +
            "INSERT INTO foo(a) VALUES(5);\n\n",
            dump());
    }
    
    public void testSeveralColumns() throws Exception {
        database.execute("create table foo(a integer, b integer)");
        database.execute("insert into foo(a, b) values(5, 8)");
        assertEquals("CREATE TABLE foo(\n  a INTEGER,\n  b INTEGER\n);\n\n" +
            "INSERT INTO foo(a, b) VALUES(5, 8);\n\n",
            dump());
    }
    
    public void testSeveralRows() throws Exception {
        database.execute("create table foo(a integer)");
        database.execute("insert into foo(a) values(5)");
        database.execute("insert into foo(a) values(6)");
        assertEquals("CREATE TABLE foo(\n  a INTEGER\n);\n\n" +
            "INSERT INTO foo(a) VALUES(5);\n" +
            "INSERT INTO foo(a) VALUES(6);\n\n",
            dump());
    }
    
    public void testRowsForSeveralTables() throws Exception {
        database.execute("create table foo(a integer)");
        database.execute("create table empty(a integer)");
        database.execute("create table bar(a integer)");
        database.execute("insert into foo(a) values(5)");
        database.execute("insert into bar(a) values(51)");
        database.execute("insert into bar(a) values(52)");
        assertEquals(
            "CREATE TABLE foo(\n  a INTEGER\n);\n\n" +
            "CREATE TABLE empty(\n  a INTEGER\n);\n\n" +
            "CREATE TABLE bar(\n  a INTEGER\n);\n" +
            "\n" +
            "INSERT INTO foo(a) VALUES(5);\n" +
            "\n" +
            "INSERT INTO bar(a) VALUES(51);\n" +
            "INSERT INTO bar(a) VALUES(52);\n" +
            "\n",
            dump());
    }
    
    public void testDataOfVariousTypes() throws Exception {
        database.execute("create table foo(a bigint, b decimal(23,1)," +
            "c varchar(255), d date, e timestamp)");
        database.execute("insert into foo values(" +
            "888111222333, 999888111222333.5, 'c''est', '2004-11-04'," +
            " '2000-02-29 13:45:01' )");
        assertEquals("INSERT INTO foo(a, b, c, d, e) VALUES(" +
            "888111222333, 999888111222333.5, 'c''est', '2004-11-04', " +
            "'2000-02-29 13:45:01');\n\n", 
            dumpData());
    }
    
    public void testBinaryData() throws Exception {
        /* output of type binary
           mysqldump outputs roughly:
             INSERT INTO foo VALUES ('b\0\0');
           which doesn't really solve the problem if \ is not special
        */
        /*
           Postgres has its own charming syntax for bytea:
           INSERT INTO foo VALUES('b\\000\\000');
         */
        /*
           According to Postgres docs, the SQL standard has a syntax
           for BLOB (different from postgres bytea).
         */
        /*
           Hypersonic is hex in single quotes, e.g. '620000'
         */
        /* Faced with this I think I need to create my own syntax.
           These are all insane. */

        database.execute("create table foo(a blob)");
        PreparedStatement statement = 
            database.openConnection().prepareStatement(
                "insert into foo(a) values(?)");
        statement.setBytes(1, new byte[] {
            0, 1, 2, 127, 77, (byte) 200, (byte) 255, 0});
        statement.executeUpdate();
        
        assertEquals("INSERT INTO foo(a) VALUES(undumpable_data);\n\n", 
            dumpData());
    }

    public void testNullAndDefault() throws Exception {
        database.execute("create table foo(a integer default 5, b integer)");
        database.execute("insert into foo() values()");
        assertEquals(
            "CREATE TABLE foo(\n  a INTEGER DEFAULT 5,\n  b INTEGER\n);\n\n" +
            "INSERT INTO foo(a, b) VALUES(5, null);\n\n",
            dump());
    }
    
    public void testNotNull() throws Exception {
        database.execute("create table foo(a integer not null)");
        assertEquals(
            "CREATE TABLE foo(\n  a INTEGER NOT NULL\n);\n\n",
            dump());
    }
    
    public void testPrimaryKey() throws Exception {
        database.execute("create table foo(a integer primary key)");
        database.execute("create table bar(" +
            "a integer, b integer, primary key(a, b))");
        assertEquals(
            "CREATE TABLE foo(\n  a INTEGER,\n  PRIMARY KEY(a)\n);\n\n" +
            "CREATE TABLE bar(\n" +
            "  a INTEGER,\n  b INTEGER,\n  PRIMARY KEY(a, b)\n);\n\n",
            dump());
    }
    
    public void testUnique() throws Exception {
        database.execute("create table foo(a integer, b integer, c integer," +
            "unique(a), unique(b, c))");
        assertEquals(
            "CREATE TABLE foo(\n  a INTEGER,\n  b INTEGER,\n  c INTEGER,\n" +
                "  UNIQUE(a),\n  UNIQUE(b, c)\n);\n\n",
            dump());
    }
    
    public void testConstraintNames() throws Exception {
        database.execute("create table name(a integer primary key)");
        database.execute("create table name2(" +
            "a integer, b integer, c integer," +
            "constraint a_key foreign key(a) references name(a)," +
            "constraint b_key primary key(b)," +
            "constraint c_uniq unique(c))");
        assertEquals(
            "CREATE TABLE name(\n  a INTEGER,\n  PRIMARY KEY(a)\n);\n\n" +
            "CREATE TABLE name2(\n  a INTEGER,\n  b INTEGER,\n  c INTEGER,\n" +
                "  CONSTRAINT a_key FOREIGN KEY(a) REFERENCES name(a),\n" +
                "  CONSTRAINT b_key PRIMARY KEY(b),\n" +
                "  CONSTRAINT c_uniq UNIQUE(c)\n);\n\n",
            dump());
    }
    
    public void testRoundTrip() throws Exception {
        database.execute("create table foo(a integer default 5," +
                "b varchar(255) not null," +
                "c bigint default 88 not null," +
                "d decimal(7,1)," +
                "e timestamp default current_timestamp," +
                "primary key(b, c)," +
                "unique(d))");
        database.execute("insert into foo(b) values('hi')");

        database.execute("create table bar(a bigint, b decimal(23,1)," +
            "c varchar(255), d date, e timestamp)");
        database.execute("insert into bar values(" +
            "888111222333, 999888111222333.5, 'c''est', '2004-11-04'," +
            " '2000-02-29 13:45:01' )");

        database.execute("create table name(a integer primary key)");
        database.execute("create table name2(" +
            "a integer, b integer, c integer," +
            "constraint a_key foreign key(a) references name(a)," +
            "constraint b_key primary key(b)," +
            "constraint c_uniq unique(c))");

        // Optionally load the large SQL file of your choice here
        
        checkRoundTrip(database.dataStore());
    }

    /**
     * From a datastore, dump it, then load from that dump,
     * dump again, and compare the two dumps.
     * 
     * This is a somewhat weak test in that if the dump does something wrong,
     * it quite possibly will do the same thing wrong in both dumps.  But if the
     * dump produces SQL we can't parse or something of that order, we'll
     * catch it.
     */
    private static void checkRoundTrip(DataStore inputStore) {
        String dump = new SqlDumper().dump(inputStore);
        Database database2 = new Database();
        try {
            database2.executeScript(new StringReader(dump));
        }
        catch (MayflyException e) {
            throw new RuntimeException(
                "failure in command: " + e.failingCommand(), e);
        }
        
        String dump2 = new SqlDumper().dump(database2.dataStore());
        assertEquals(dump, dump2);
    }

    private String dumpData() throws IOException {
        StringWriter out = new StringWriter();
        new SqlDumper().data(database.dataStore(), out);
        return out.toString();
    }
    
    // auto-increment: can dump out and get the same next value on restore

    private String dump() {
        return new SqlDumper().dump(database.dataStore());
    }
    
}