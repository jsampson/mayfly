package net.sourceforge.mayfly.acceptance;

import java.sql.SQLException;

public class AutoIncrementTest extends SqlTestCase {
    
    public void testAutoUnderbarIncrement() throws Exception {
        String sql = "create table foo (x integer primary key auto_increment, y varchar(255))";
        if (dialect.haveAutoUnderbarIncrement()) {
            execute(sql);
            check();
        }
        else {
            expectExecuteFailure(sql, "expected ')' but got auto_increment");
        }
    }

    public void testSerial() throws Exception {
        String sql = "create table foo (x serial, y varchar(255))";
        if (dialect.haveSerial()) {
            execute(sql);
            check();
        }
        else {
            expectExecuteFailure(sql, "expected data type but got serial");
        }
    }
    
    public void testIdentity() throws Exception {
        String sql = "create table foo (x identity, y varchar(255))";
        if (dialect.haveIdentity()) {
            execute(sql);
            check();
        }
        else {
            expectExecuteFailure(sql, "expected data type but got identity");
        }
    }
    
    public void testSql200x() throws Exception {
        // At least, what hypersonic calls SQL 200x syntax...
        // could also test START WITH and INCREMENT BY
        String sql = "create table foo (x INTEGER GENERATED BY DEFAULT " +
            "AS IDENTITY(START WITH 1) PRIMARY KEY, y varchar(255))";
        if (dialect.haveSql200xAutoIncrement()) {
            execute(sql);
            check();
        }
        else {
            expectExecuteFailure(sql, "expected ')' but got GENERATED");
        }
    }

    public void testGeneratedByDefaultNoStart() throws Exception {
        String sql = "create table foo (x INTEGER GENERATED BY DEFAULT " +
            "AS IDENTITY PRIMARY KEY, y varchar(255))";
        if (dialect.haveSql200xAutoIncrement()) {
            execute(sql);
            check();
        }
        else {
            expectExecuteFailure(sql, "expected ')' but got GENERATED");
        }
    }
    
    // Derby also has GENERATED ALWAYS

    private void check() throws SQLException {
        execute("insert into foo(x, y) values (92, 'a')");
        execute("insert into foo(y) values ('b')");
        execute("insert into foo(y) values ('c')");
        assertResultSet(
            dialect.autoIncrementIsRelativeToLastValue()
                ?
                new String[] {
                    " 92, 'a' ",
                    " 93, 'b' ",
                    " 94, 'c' "
                }
                :
                new String[] {
                    " 92, 'a' ",
                    " 1, 'b' ",
                    " 2, 'c' "
                }
            ,
            query("select x, y from foo")
        );
    }

    // Not valid in MySQL because this isn't a key:
    // execute("create table foo (x integer not null auto_increment, y varchar(255))");
    // On the other hand postgres does not require a primary key or unique constraint for serial
    
    // serial in postgres does imply not null
    
    // MySQL doesn't allow more than one auto-increment column
    
    // Inserting null implies an auto-generated value (think this is true of hypersonic,mysql. others?

}