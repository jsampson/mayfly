package net.sourceforge.mayfly.acceptance;

import java.sql.*;

public class AggregateTest extends SqlTestCase {
    
    public void testBasics() throws Exception {
        execute("create table foo (x integer)");
        execute("insert into foo (x) values (5)");
        execute("insert into foo (x) values (null)");
        execute("insert into foo (x) values (9)");
        
        assertResultSet(new String[] { " 9 " }, query("select max(x) from foo"));
        assertResultSet(new String[] { " 5 " }, query("select min(x) from foo"));
        assertResultSet(new String[] { " 2 " }, query("select count(x) from foo"));
        assertResultSet(new String[] { " 3 " }, query("select count(*) from foo"));
        assertResultSet(new String[] { " 14 " }, query("select sum(x) from foo"));
        assertResultSet(new String[] { " 7 " }, query("select avg(x) from foo"));
    }

    public void testColumnAndAggregate() throws Exception {
        execute("create table foo (x integer)");
        execute("insert into foo (x) values (5)");
        
        expectQueryFailure("select x, max(x) from foo", "x is a column but max(x) is an aggregate");
        expectQueryFailure("select X || 'L', Max ( x ) from foo", "X is a column but Max(x) is an aggregate");
        expectQueryFailure("select '#' || x , MAX(X) from foo", "x is a column but MAX(X) is an aggregate");
        expectQueryFailure("select max(x) || 'L', x from foo", "x is a column but max(x) is an aggregate");
        expectQueryFailure("select '#' || max(x) , x from foo", "x is a column but max(x) is an aggregate");
    }
    
    public void testColumnAndCountAll() throws Exception {
        execute("create table foo (x integer)");
        expectQueryFailure("select x, coUNt ( * ) from foo", "x is a column but coUNt(*) is an aggregate");
    }

    public void testLiteralAndAggregate() throws Exception {
        execute("create table foo (x integer)");
        execute("insert into foo (x) values (5)");
        
        assertResultSet(new String[] { " 3, 5 " }, query("select 3, max(x) from foo")); 
    }

    public void testLiteralAndColumn() throws Exception {
        execute("create table foo (x integer)");
        execute("insert into foo (x) values (5)");
        
        assertResultSet(new String[] { " 3, 5 " }, query("select 3, x from foo")); 
    }

    public void testBadColumnName() throws Exception {
        execute("create table foo (x integer)");
        execute("insert into foo (x) values (5)");
        execute("insert into foo (x) values (null)");
        execute("insert into foo (x) values (9)");
        
        expectQueryFailure("select max(y) from foo", "no column y");
    }

    public void testWhere() throws Exception {
        execute("create table foo (x integer, y integer)");
        execute("insert into foo (x, y) values (5, 10)");
        execute("insert into foo (x, y) values (null, 10)");
        execute("insert into foo (x, y) values (9, 9)");
        
        assertResultSet(new String[] { " 5 " }, query("select max(x) from foo where y = 10"));
    }

    public void testNoRows() throws Exception {
        execute("create table foo (x integer)");
        assertResultSet(new String[] { " null " }, query("select max(x) from foo"));
        assertResultSet(new String[] { " null " }, query("select min(x) from foo"));
        assertResultSet(new String[] { " 0 " }, query("select count(x) from foo"));
        assertResultSet(new String[] { " 0 " }, query("select count(*) from foo"));
        assertResultSet(new String[] { " null " }, query("select sum(x) from foo"));
        assertResultSet(new String[] { " null " }, query("select avg(x) from foo"));

        expectQueryFailure("select max(y) from foo", "no column y");
    }

    public void testNullRowsOnly() throws Exception {
        execute("create table foo (x integer)");
        execute("insert into foo (x) values (null)");
        assertResultSet(new String[] { " null " }, query("select max(x) from foo"));
        assertResultSet(new String[] { " null " }, query("select min(x) from foo"));
        assertResultSet(new String[] { " 0 " }, query("select count(x) from foo"));
        assertResultSet(new String[] { " 1 " }, query("select count(*) from foo"));
        assertResultSet(new String[] { " null " }, query("select sum(x) from foo"));
        assertResultSet(new String[] { " null " }, query("select avg(x) from foo"));

        expectQueryFailure("select max(y) from foo", "no column y");
    }

    public void testAggregateExpression() throws Exception {
        execute("create table foo (x integer)");
        execute("insert into foo (x) values (5)");
        
        if (dialect.verticalBarsMeanConcatenation()) {
            assertResultSet(new String[] { " 'L5' " }, query("select 'L' || max(x) from foo"));
        } else {
            assertResultSet(new String[] { " 6 " }, query("select 1 + max(x) from foo"));
        }
        expectQueryFailure("select 'L' || max(y) from foo", "no column y");
    }

    public void testCountDistinctAndAll() throws Exception {
        execute("create table foo (x integer, y integer)");
        execute("insert into foo (x, y) values (5, 60)");
        execute("insert into foo (x, y) values (5, 90)");
        execute("insert into foo (x, y) values (7, 90)");
        
        assertResultSet(new String[] { " 3 " }, query("select count(all x) from foo"));
        assertResultSet(new String[] { " 2 " }, query("select count(distinct x) from foo"));
    }

    public void testAll() throws Exception {
        execute("create table foo (x integer, y integer)");
        execute("insert into foo (x, y) values (5, 60)");
        execute("insert into foo (x, y) values (5, 90)");
        execute("insert into foo (x, y) values (7, 90)");
        
        assertResultSet(new String[] { " 80 " }, query("select avg(all y) from foo"));
        assertResultSet(new String[] { " 17 " }, query("select sum(all x) from foo"));
        assertResultSet(new String[] { " 5 " }, query("select min(all x) from foo"));
        assertResultSet(new String[] { " 7 " }, query("select max(all x) from foo"));
    }

    public void testDistinct() throws Exception {
        execute("create table foo (x integer, y integer)");
        execute("insert into foo (x, y) values (5, 60)");
        execute("insert into foo (x, y) values (5, 90)");
        execute("insert into foo (x, y) values (7, 90)");
        
        checkDistinct(75, "select avg(distinct y) from foo");

        checkDistinct(12, "select sum(distinct x) from foo");

        // Minimum/maximum are kind of pointless, but legal it would seem
        checkDistinct(5, "select min(distinct x) from foo");
        checkDistinct(7, "select max(distinct x) from foo");
    }

    private void checkDistinct(int expected, String sql) throws SQLException {
        if (dialect.aggregateDistinctIsForCountOnly()) {
            expectQueryFailure(sql, null);
        } else {
            assertResultSet(new String[] { "" + expected }, query(sql));
        }
    }
    
    public void testAsteriskOnlyForCount() throws Exception {
        execute("create table foo (x integer, y integer)");
        if (!dialect.aggregateAsteriskIsForCountOnly()) {
            // Hypersonic has a variety of behaviors, depending on whether there
            // are any rows, and which function.  None of them seem very useful.
            return;
        }
        expectQueryFailure("select avg(*) from foo", "expected primary but got ASTERISK");
        expectQueryFailure("select sum(*) from foo", "expected primary but got ASTERISK");
        expectQueryFailure("select min(*) from foo", "expected primary but got ASTERISK");
        expectQueryFailure("select max(*) from foo", "expected primary but got ASTERISK");
    }

}