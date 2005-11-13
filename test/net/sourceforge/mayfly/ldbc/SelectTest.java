package net.sourceforge.mayfly.ldbc;

import junit.framework.*;

import net.sourceforge.mayfly.datastore.*;
import net.sourceforge.mayfly.ldbc.what.*;
import net.sourceforge.mayfly.ldbc.where.*;
import net.sourceforge.mayfly.ldbc.where.literal.*;
import net.sourceforge.mayfly.util.*;

import java.sql.*;

public class SelectTest extends TestCase {
    public void testGrandParseIntegration() throws Exception {
        assertEquals(
            new Select(
                new What()
                    .add(new AllColumnsFromTable("f"))
                    .add(new SingleColumnExpression("b", "name")),
                new From()
                    .add(new FromTable("foo", "f"))
                    .add(new FromTable("bar", "b")),
                new Where(
                    new And(
                        new And(
                            new Eq(new SingleColumnExpression("f", "name"), new QuotedString("'steve'")),
                            new Or(
                                new Eq(new SingleColumnExpression("size"), new MathematicalInt(4)),
                                new Gt(new MathematicalInt(6), new SingleColumnExpression("size"))    
                            )

                        ),
                        new Or(
                            new Eq(new SingleColumnExpression("color"), new QuotedString("'red'")),
                            new And(
                                new Not(new Eq(new SingleColumnExpression("day"), new MathematicalInt(7))),
                                new Not(new Eq(new SingleColumnExpression("day"), new MathematicalInt(6)))
                            )

                        )
                    )

                )
            ),
            Select.selectFromTree(Tree.parse("select f.*, b.name from foo f, bar b " +
                                       "where (f.name='steve' and " +
                                                " (size = 4 or 6 >size ) ) " +
                                             " and " +
                                                 "(color='red' or " +
                                                            " (day <>7 and day != 6) )"))
        );
    }
    
    public void testParseIntegerLiteral() throws Exception {
        assertEquals(
            new Select(
                new What()
                    .add(new All()),
                new From()
                    .add(new FromTable("foo")),
                new Where(
                    new Eq(new SingleColumnExpression("a"), new MathematicalInt(5))
                )
            ),
            Select.selectFromTree(Tree.parse("select * from foo where a = 5"))
        );
    }

    public void testAliasOmitted() throws Exception {
        assertEquals(
            new Select(
                new What()
                    .add(new SingleColumnExpression("name")),
                new From()
                    .add(new FromTable("foo")),
                Where.EMPTY
            ),
            Select.selectFromTree(Tree.parse("select name from foo"))
        );
    }

    public void testParseSelectExpression() throws Exception {
        assertEquals(
            new Select(
                new What()
                    .add(new MathematicalInt(5)),
                new From()
                    .add(new FromTable("foo")),
                Where.EMPTY
            ),
            Select.selectFromTree(Tree.parse("select 5 from foo"))
        );
    }

    public void testParseJdbcParameter() throws Exception {
        assertEquals(
            new Select(
                new What()
                    .add(JdbcParameter.INSTANCE),
                new From()
                    .add(new FromTable("foo")),
                new Where(
                    new Eq(new SingleColumnExpression("a"), JdbcParameter.INSTANCE)
                )
            ),
            Select.selectFromTree(Tree.parse("select ? from foo where a = ?"))
        );
    }

    public void testParameterCount() throws Exception {
        checkParameterCount(2, "select ? from foo where a = ?");
        checkParameterCount(3, "select a from foo where (? = b or ? != c) and a > ?");
        checkParameterCount(2, "select a from foo where ? IN (1, ?, 5)");
    }

    private void checkParameterCount(int expected, String sql) throws SQLException {
        assertEquals(expected, Select.selectFromTree(Tree.parse(sql)).parameterCount());
    }
    
    public void testSubstituteMultipleValues() throws Exception {
        assertEquals(
            new Select(
                new What()
                    .add(new MathematicalInt(5)),
                new From()
                    .add(new FromTable("foo")),
                new Where(
                    new And(
                        new Or(
                            new Eq(
                                new SingleColumnExpression("a"),
                                new MathematicalInt(6)
                            ),
                            new Not(
                                new Eq(
                                    new MathematicalInt(7),
                                    new SingleColumnExpression("b")
                                )
                            )
                        ),
                        new Gt(
                            new MathematicalInt(8),
                            new SingleColumnExpression("c")
                        )
                    )
                )
            ),
            substitute("select ? from foo where (a = ? or ? != b) and c < ?")
        );
    }

    public void testSubstituteIn() throws Exception {
        assertEquals(
            new Select(
                new What()
                    .add(new SingleColumnExpression("a")),
                new From()
                    .add(new FromTable("foo")),
                new Where(
                    new In(
                        new MathematicalInt(5),
                        L.fromArray(new Object[] {
                            new MathematicalInt(6),
                            new MathematicalInt(3),
                            new MathematicalInt(7)
                        })
                    )
                )
            ),
            substitute("select a from foo where ? in (?, 3, ?)")
        );
    }

    private Command substitute(String sql) throws SQLException {
        Command command = Command.fromTree(Tree.parse(sql));
        command.substitute(L.fromArray(new int[] { 5, 6, 7, 8 }));
        return command;
    }

    // Evidently, X is reserved to ldbc (but not jsqlparser)
    public void testX() throws Exception {
        try {
            Select.selectFromTree(Tree.parse("select x from foo"));
            fail();
        } catch (SQLException e) {
            assertEquals("unexpected token: x", e.getMessage());
        }
    }
    
    public void testParseExplicitJoin() throws Exception {
        assertEquals(
            new Select(
                new What()
                    .add(new All()),
                new From()
                    .add(new Join(
                        new FromTable("places"),
                        new FromTable("types"),
                        new Where(
                            new Eq(new SingleColumnExpression("type"), new SingleColumnExpression("id"))
                        )
                    )),
                Where.EMPTY
            ),
            Select.selectFromTree(Tree.parse(
                "select * from places inner join types on type = id"
            ))
        );
    }
    
    public void xtestNestedJoins() throws Exception {
        // There's just no sane way to do this currently.  I think the grammar probably has
        // to be smarter about what groups with what (?)
        System.out.println(Tree.parse(
                "select * from foo inner join bar on f = b1 inner join quux on b2 = q"
            ).toString());
        assertEquals(
            new Select(
                new What()
                    .add(new All()),
                new From()
                    .add(new Join(
                        new Join(
                            new FromTable("foo"),
                            new FromTable("bar"),
                            new Where(
                                new Eq(new SingleColumnExpression("f"), new SingleColumnExpression("b1"))
                            )
                        ),
                        new FromTable("types"),
                        new Where(
                            new Eq(new SingleColumnExpression("b2"), new SingleColumnExpression("q"))
                        )
                    )),
                Where.EMPTY
            ),
            Select.selectFromTree(Tree.parse(
                "select * from foo inner join bar on f = b1 inner join quux on b2 = q"
            ))
        );
    }

    public void testExecuteSimpleJoin() throws Exception {
        DataStore store =
            new DataStore()
                .createTable("foo", new L().append("colA").append("colB"))
                .addRow("foo", new L().append("colA").append("colB"), new L().append("1a").append("1b"))
                .addRow("foo", new L().append("colA").append("colB"), new L().append("2a").append("2b"))
                .createTable("bar", new L().append("colX").append("colY"))
                .addRow("bar", new L().append("colX").append("colY"), new L().append("1a").append("1b"))
                .addRow("bar", new L().append("colX").append("colY"), new L().append("2a").append("2b"))
                .addRow("bar", new L().append("colX").append("colY"), new L().append("3a").append("3b"));


        assertEquals(
            store.table("foo").rows().cartesianJoin(store.table("bar").rows()),
            Select.selectFromTree(Tree.parse("select * from foo, bar")).query(store)
        );
    }

    public void testSmallerJoin() throws Exception {
        DataStore store =
            new DataStore()
                .createTable("foo", new L().append("colA"))
                .addRow("foo", new L().append("colA"), new L().append("1a"))
                .createTable("bar", new L().append("colX"))
                .addRow("bar", new L().append("colX"), new L().append("barXValue"))
                ;


        assertEquals(
            new Rows(
                new L()
                    .append(new Row(new M()
                            .entry(new Column("bar", "colX"), new Cell("barXValue"))
                            .entry(new Column("foo", "colA"), new Cell("1a"))
                            .asImmutable())
                ).asImmutable()
            ),
            Select.selectFromTree(Tree.parse("select * from foo, bar")).query(store)
        );
    }

    public void testSimpleWhere() throws Exception {
        DataStore store =
            new DataStore()
                .createTable("foo", new L().append("colA").append("colB"))
                .addRow("foo", new L().append("colA").append("colB"), new L().append("1a").append("1b"))
                .addRow("foo", new L().append("colA").append("colB"), new L().append("2a").append("xx"))
                .addRow("foo", new L().append("colA").append("colB"), new L().append("3a").append("xx"));

        assertEquals(
            store.table("foo").rows().elements(new int[]{1, 2}),
            Select.selectFromTree(Tree.parse("select * from foo where colB = 'xx'")).query(store)
        );
    }


    //TODO: probably need to resolve columns to be fully qualified, i.e. table + string
    
}
