package net.sourceforge.mayfly.acceptance;

public class SubselectTest extends SqlTestCase {
    
    public void testAggregate() throws Exception {
        execute("create table foo(x integer, name varchar(10))");
        execute("insert into foo(x, name) values(6, 'six')");
        execute("insert into foo(x, name) values(5, 'five')");
        execute("insert into foo(x, name) values(4, 'four')");

        execute("create table bar(y integer)");
        execute("insert into bar(y) values(5)");
        execute("insert into bar(y) values(2)");
        execute("insert into bar(y) values(-7)");

        assertResultSet(new String[] { " 'five' " },
            query("select name from foo where x = (select max(y) from bar)"));
    }
    
    /**
     * @internal
     * The subselect doesn't need to be an aggregate; anything which
     * returns a single row will do.
     * Similar to the technique in {@link ResultTest#testTopNQuery()}
     */
    public void testOneRow() throws Exception {
        execute("create table countries(id integer, name varchar(255))");
        execute("insert into countries values(1, 'Australia')");
        execute("insert into countries values(2, 'Sri Lanka')");
        execute("insert into countries values(3, 'India')");
        
        execute("create table cities(country integer, name varchar(80))");
        execute("insert into cities(country, name) values (1, 'Perth')");
        execute("insert into cities(country, name) values (3, 'Mumbai')");
        
        assertResultSet(new String[] { " 'Australia' " },
            query("select name from countries where id = " +
                "(select country from cities where name = 'Perth')"));
    }
    
    /* Similar case but the subselect has a reference to the foo row.
    */

}
