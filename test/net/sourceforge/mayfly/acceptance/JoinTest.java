package net.sourceforge.mayfly.acceptance;

import java.sql.ResultSet;

public class JoinTest extends SqlTestCase {

    public void testImplicitInnerJoin() throws Exception {
        execute("create table foo (a integer)");
        execute("create table bar (b integer)");
        execute("insert into foo (a) values (4)");
        execute("insert into foo (a) values (5)");
        execute("insert into bar (b) values (100)");
        execute("insert into bar (b) values (101)");

        assertResultSet(
            new String[] {
                "   4,  100 ",
                "   4,  101 ",
                "   5,  100 ",
                "   5,  101 ",
            },
            query("select foo.a, bar.b from foo, bar")
        );
    }

    public void testJoinSameNameTwice() throws Exception {
        execute("create table foo (a integer)");
        execute("create table bar (a integer)");
        execute("insert into foo (a) values (4)");
        execute("insert into foo (a) values (5)");
        execute("insert into bar (a) values (100)");
        execute("insert into bar (a) values (101)");
        assertResultSet(
            new String[] {
                "   4,  100 ",
                "   4,  101 ",
                "   5,  100 ",
                "   5,  101 ",
            },
            query("select foo.a, bar.a from foo, bar")
        );
    }

    public void testWhereNeedsTableName() throws Exception {
        execute("create table foo (a integer)");
        execute("create table bar (a integer)");
        execute("insert into foo (a) values (4)");
        execute("insert into bar (a) values (100)");
        execute("insert into bar (a) values (101)");

        assertResultSet(
            new String[] { "4, 100" },
            query("select foo.a, bar.a from foo, bar where bar.a = 100")
        );
    }

    public void testColumnNameForWrongTable() throws Exception {
        execute("CREATE TABLE FOO (A INTEGER)");
        execute("CREATE TABLE bar (b INTEGER)");
        expectQueryFailure("select foo.b from foo, bar", "no column foo.b");

        expectQueryFailure("select a from foo, bar where bar.A = 5", "no column bar.A");

        execute("insert into FOO (a) values (7)");
        execute("insert into bar (b) values (8)");
        expectQueryFailure("select a from foo, bar where bar.A = 5", "no column bar.A");
    }

    public void testAmbiguousColumnName() throws Exception {
        execute("CREATE TABLE foo (A INTEGER)");
        execute("CREATE TABLE bar (a INTEGER)");
        execute("insert into foo (a) values (5)");
        execute("insert into bar (a) values (9)");
        
        String ambiguousColumnNameQuery = "select A from foo, bar";
        if (dialect.detectsAmbiguousColumns()) {
            expectQueryFailure(ambiguousColumnNameQuery, "ambiguous column A");
        } else {
            assertResultSet(new String[] {"5"}, query(ambiguousColumnNameQuery));
            assertResultSet(new String[] {"9"}, query("select A from bar, foo"));
        }
    }
    
    public void testAmbiguousEvenWithJoins() throws Exception {
        // The main point here is that the query optimizer shouldn't
        // "optimize" a to be foo.a
        execute("create table foo (a integer)");
        execute("create table bar (b integer)");
        execute("create table baz (a integer)");
        String sql = "select * from foo, bar, baz where a = 5";
        if (dialect.detectsAmbiguousColumns()) {
            expectQueryFailure(sql, "ambiguous column a");
        }
        else {
            assertResultSet(new String[] { }, query(sql));
        }
    }
    
    public void testNeedThirdTable() throws Exception {
        // The main point here is that the query optimizer
        // cannot join tables foo and bar first, and also
        // apply the where on that first join.
        execute("create table foo (a integer)");
        execute("create table bar (b integer)");
        execute("create table baz (a integer)");
        
        execute("insert into foo(a) values (3)");
        execute("insert into bar(b) values (4)");
        execute("insert into baz(a) values (5)");
        execute("insert into baz(a) values (6)");

        assertResultSet(
            new String[] { " 3, 4, 5 " }, 
            query("select * from foo, bar, baz where baz.a = 5"));
    }

    public void testAlias() throws Exception {
        execute("create table foo (a integer)");
        execute("insert into foo (a) values (4)");
        execute("insert into foo (a) values (10)");
        ResultSet results = query("select f.a from foo f where f.a = 4");
        assertTrue(results.next());

        assertEquals(4, results.getInt("a"));

        assertFalse(results.next());
    }

    public void testAliasResolvesToCorrectTable() throws Exception {
        execute("create table foo (a integer)");
        execute("create table bar (a integer)");
        execute("insert into foo (a) values (4)");
        execute("insert into bar (a) values (100)");
        execute("insert into bar (a) values (101)");

        assertResultSet(
            new String[] { "4, 100" },
            query("select f.a, b.a from foo f, bar b where b.a = 100")
        );
    }
    
    public void testSelfJoin() throws Exception {
        execute("create table place (id integer, parent integer, name varchar(80))");
        execute("insert into place (id, parent, name) values (1, 0, 'India')");
        execute("insert into place (id, parent, name) values (10, 1, 'Karnataka')");
        execute("insert into place (id, parent, name) values (100, 10, 'Bangalore')");
        assertResultSet(
            new String[] {
                " 'Karnataka', 'India' ",
                " 'Bangalore', 'Karnataka' ",
            },
            query("select child.name, parent.name from place parent, place child " +
                "where parent.id = child.parent")
        );
    }

    public void testLeftJoin() throws Exception {
        execute("create table place (id integer, parent integer, name varchar(80))");
        execute("insert into place (id, parent, name) values (1, 0, 'India')");
        execute("insert into place (id, parent, name) values (10, 1, 'Karnataka')");
        execute("insert into place (id, parent, name) values (100, 10, 'Bangalore')");
        assertResultSet(
            new String[] {
                " 'India', null ",
                " 'Karnataka', 'India' ",
                " 'Bangalore', 'Karnataka' ",
            },
            query("select child.name, parent.name from place child left outer join place parent " +
                "on parent.id = child.parent")
        );
    }
    
    public void testWordOuterIsOptional() throws Exception {
        execute("create table foo (a integer)");
        execute("create table bar (a integer)");
        assertNotNull(query("select * from foo left join bar on 1 = 1"));
    }

    public void testOuterSelfJoin() throws Exception {
        execute("create table foo (Id integer, parent integer)");
        assertResultSet(new String[] { }, 
            query("select * from foo child left outer join foo parent on child.parent = parent.id"));
    }
    
    public void testDuplicateAliasWithDifferingColumnNames() throws Exception {
        execute("create table foo(a integer)");
        execute("create table bar(b integer)");
        execute("insert into foo(a) values(5)");
        execute("insert into bar(b) values(9)");

        String sql = "select * from foo t, bar t";
        if (dialect.allowDuplicateTableWithDifferingColumnNames()) {
            /* I guess it doesn't really matter whether the database
               returns 5,5 or 5,9.  Either way, accepting this seems
               dubious. */
            query(sql);
        }
        else {
            expectQueryFailure(sql, "duplicate table name or alias t");
        }
    }

    public void testDuplicateAliasWithSameColumnNames() throws Exception {
        execute("create table foo(a integer)");
        execute("create table bar(a integer)");
        execute("insert into foo(a) values(5)");
        execute("insert into bar(a) values(9)");

        String sql = "select * from foo t, bar t";
        if (dialect.allowDuplicateTableInQuery()) {
            /**
             * Presumably the same buggyness/strangeness as
             * {@link Dialect#detectsAmbiguousColumns()}.
             */
            assertResultSet(new String[] { " 5, 5 " }, query(sql));
        }
        else {
            expectQueryFailure(sql, "duplicate table name or alias t");
        }
    }

    /**
     * Basically the same case as 
     * {@link #testDuplicateAliasWithSameColumnNames()}.
     */
    public void testAmbiguousColumnViaJoin() throws Exception {
        execute("create table foo (a integer)");
        String sql = "select * from foo left outer join foo on 1 = 1";
        if (dialect.allowDuplicateTableInQuery()) {
            assertResultSet(new String[] { }, query(sql));
        }
        else {
            expectQueryFailure(sql, "duplicate table name or alias foo");
        }
    }
    
    public void testCrossJoin() throws Exception {
        // Hypersonic, Derby, and to a certain extent MySQL, treat CROSS JOIN as being
        // just like INNER JOIN.  Mayfly, Oracle, and Postgres hew more closely
        // to the SQL standard: INNER JOIN must have ON and CROSS JOIN cannot have ON.

        execute("create table foo (a integer)");
        execute("create table bar (b integer)");
        execute("insert into foo (a) values (4)");
        execute("insert into foo (a) values (5)");
        execute("insert into bar (b) values (100)");
        execute("insert into bar (b) values (101)");

        String[] fullCartesianProduct = new String[] {
            "   4,  100 ",
            "   4,  101 ",
            "   5,  100 ",
            "   5,  101 ",
        };

        String crossJoinNoOn = "select a, b from foo cross join bar";
        if (dialect.crossJoinRequiresOn()) {
            expectQueryFailure(crossJoinNoOn, null);
        } else {
            assertResultSet(fullCartesianProduct, query(crossJoinNoOn));
        }
        
        String crossJoinWithOn = "select a, b from foo cross join bar on 1 = 1";
        if (dialect.crossJoinCanHaveOn()) {
            assertResultSet(fullCartesianProduct, query(crossJoinWithOn));
        } else {
            expectQueryFailure(crossJoinWithOn,
                // This message might be worthwhile, but I'm not sure whether the
                // parser should be trying to guess that an ON goes with a CROSS JOIN.
                // Especially in a dangling ON situation that might create other problems.
//                "Specify INNER JOIN, not CROSS JOIN, if you want an ON condition"
                
                "expected end of file but got ON"
            );
        }

        String innerJoinNoOn = "select a, b from foo inner join bar";
        if (dialect.innerJoinRequiresOn()) {
            expectQueryFailure(innerJoinNoOn, 
                // Might not be too hard to produce this error message but would it
                // really be right? In "FOO INNER JOIN BAR BAZ QUUX ON A = B" is
                // the ON omitted or is QUUX just an extraneous token?
//                "Specify CROSS JOIN, not INNER JOIN, if you want to omit an ON condition"

                "expected ON but got end of file"
            );
        } else {
            assertResultSet(fullCartesianProduct, query(innerJoinNoOn));
        }
    }

    public void testExplicitJoin() throws Exception {
        execute("create table places (name varchar(80), type integer)");
        execute("create table types (type integer, name varchar(80))");
        execute("insert into places (name, type) values ('London', 1)");
        execute("insert into places (name, type) values ('France', 2)");
        execute("insert into places (name, type) values ('Erewhon', 0)");
        execute("insert into types (name, type) values ('City', 1)");
        execute("insert into types (name, type) values ('Country', 2)");

        assertResultSet(
            new String[] {
                " 'London',   'City'    ",
                " 'France',   'Country' ",
            },
            query("select places.name, types.name from places inner join types on places.type = types.type")
        );
    }
    
    public void testErrorInOnCondition() throws Exception {
        execute("create table places (name varchar(80), type integer)");
        execute("create table types (type integer, name varchar(80))");
        expectQueryFailure(
            "select places.name from places inner join types on type = types.type",
            "ambiguous column type");
    }

    public void testCombineExplicitAndImplicitJoins() throws Exception {
        // It is useful/common to have a query with both an explicit and
        // implicit join in it?  (It is common if one is an outer join...)

        // Another case if these can be made to work:
        // from foo, bar outer join baz  => the "left" is bar, not the result of foo cross bar
        //   (or is it?)

        execute("create table foo (a integer)");
        execute("create table bar (a integer)");
        execute("create table types (type integer, name varchar(80))");
        execute("insert into foo (a) values (5)");
        execute("insert into bar (a) values (9)");
        execute("insert into bar (a) values (10)");
        execute("insert into types (name, type) values ('City', 9)");
        
        // Illustrates setup but isn't the point of this test
        assertResultSet(
            new String[] { " 9 " },
            query("select a from bar inner join types on a = type")
        );

        // Hypersonic/MySQL4 say column A is ambiguous
        // According to MySQL5 documentation, it is because MySQL4
        // parses it as "(foo, bar) inner join types", not because
        // the ON can reach outside of the joined tables as such.
        String ambiguousIfReachesOutOfJoin = "select foo.a, bar.a from foo, bar inner join types on a = type";
        if (dialect.onIsRestrictedToJoinsTables()) {
            assertResultSet(
                new String[] { " 5, 9 " },
                query(ambiguousIfReachesOutOfJoin)
            );
        } else {
            expectQueryFailure(ambiguousIfReachesOutOfJoin, "ambiguous column a");
        }

        // Portable variant of above case
        assertResultSet(
            new String[] { " 5, 9 " },
            query("select foo.a, bar.a from foo, bar inner join types on bar.a = type")
        );

        // A similar case:
        String onReachesOutOfJoinedColumnsQuery = 
            "select foo.a, bar.a from bar, foo inner join types on bar.a = type";
        if (dialect.onCanMentionOutsideTable()) {
            assertResultSet(
                new String[] { " 5, 9 " },
                query(onReachesOutOfJoinedColumnsQuery)
            );
        }
        else {
            expectQueryFailure(onReachesOutOfJoinedColumnsQuery, "no column bar.a");
        }

        String ambiguousIfOneConsidersTablesMentionedAfterJoin =
            "select foo.a, bar.a from bar inner join types on a = type, foo";
        if (dialect.considerTablesMentionedAfterJoin()) {
            expectQueryFailure(ambiguousIfOneConsidersTablesMentionedAfterJoin, 
                "ambiguous column a");
        } else {
            assertResultSet(
                new String[] { " 5, 9 " },
                query(ambiguousIfOneConsidersTablesMentionedAfterJoin)
            );
        }
        // Next would be the case just like that but where the ON explicitly says "foo.a"

    }

    public void testNestedJoins() throws Exception {
        execute("create table foo (f integer, name varchar(80))");
        execute("create table bar (b1 integer, b2 integer)");
        execute("create table quux (q integer, name varchar(80))");
        execute("insert into foo (f, name) values (5, 'FooVal')");
        execute("insert into foo (f, name) values (7, 'FooDecoy')");
        execute("insert into bar (b1, b2) values (5, 9)");
        execute("insert into bar (b1, b2) values (5, 10)");
        execute("insert into bar (b1, b2) values (4, 9)");
        execute("insert into quux (q, name) values (9, 'QuuxVal')");
        execute("insert into quux (q, name) values (8, 'QuuxDecoy')");
        
        String onsAtEnd = "select foo.name, quux.name from foo inner join bar inner join quux on b2 = q on f = b1";
        if (dialect.rightHandArgumentToJoinCanBeJoin(false)) {
            assertResultSet(
                new String[] {" 'FooVal', 'QuuxVal' " },
                query(onsAtEnd)
            );
        } else {
            expectQueryFailure(onsAtEnd, null);
        }

        String parenthesizedQuery = 
            "select foo.name, quux.name from foo inner join (bar inner join quux on b2 = q) on f = b1";
        if (dialect.rightHandArgumentToJoinCanBeJoin(true)) {
            assertResultSet(
                new String[] {" 'FooVal', 'QuuxVal' " },
                query(parenthesizedQuery)
            );
        } else {
            expectQueryFailure(parenthesizedQuery, null);
        }

        assertResultSet(
            new String[] {" 'FooVal', 'QuuxVal' " },
            query("select foo.name, quux.name from foo inner join bar on f = b1 inner join quux on b2 = q")
        );
    }
    
    public void testParenthesesAndJoins() throws Exception {
        execute("create table apple(a varchar(255))");
        execute("create table banana(b varchar(255))");
        execute("create table carrot(c varchar(255))");
        execute("insert into apple(a) values('aa')");
        execute("insert into banana(b) values('bb')");
        execute("insert into carrot(c) values('cc')");
        
        assertResultSet(new String[] { " 'aa', 'bb', 'cc' " },
            query("select a, b, c from apple, banana, carrot"));

        String simpleCrossJoin = "select a, b, c from apple cross join banana cross join carrot";
        if (dialect.crossJoinRequiresOn()) {
            expectExecuteFailure(simpleCrossJoin, "expected ON but got CROSS");
            return;
        }
        assertResultSet(new String[] { " 'aa', 'bb', 'cc' " },
            query(simpleCrossJoin));

        assertResultSet(new String[] { " 'aa', 'bb', 'cc' " },
            query("select a, b, c from apple cross join banana, carrot"));
        assertResultSet(new String[] { " 'aa', 'bb', 'cc' " },
            query("select a, b, c from apple, banana cross join carrot"));
        // Those were warmups.  Here we start in on parens
        assertResultSet(new String[] { " 'aa', 'bb', 'cc' " },
            query("select a, b, c from (apple cross join banana) cross join carrot"));
        assertResultSet(new String[] { " 'aa', 'bb', 'cc' " },
            query("select a, b, c from apple cross join (banana cross join carrot)"));
        assertResultSet(new String[] { " 'aa', 'bb', 'cc' " },
            query("select a, b, c from apple inner join (banana cross join carrot) on a = 'aa' "));
    }
    
    public void testJoinOnNull() throws Exception {
        // This case is mentioned in the documentation for
        // hypersonic 1.8.x.
        execute("create table foo (a integer, b integer)");
        execute("create table bar (b integer, c integer)");
        execute("insert into foo (a, b) values (1, 10)");
        execute("insert into bar (b, c) values (10, 100)");

        execute("insert into foo (a, b) values (2, null)");
        execute("insert into bar (b, c) values (null, 200)");
        
        assertResultSet(
            new String[] { "1, 100" },
            query("select a, c from foo inner join bar on foo.b = bar.b")
        );
    }
    
}
