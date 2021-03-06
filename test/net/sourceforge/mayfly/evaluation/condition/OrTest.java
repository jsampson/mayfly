package net.sourceforge.mayfly.evaluation.condition;

import junit.framework.TestCase;

import net.sourceforge.mayfly.datastore.Row;
import net.sourceforge.mayfly.datastore.StringCell;
import net.sourceforge.mayfly.datastore.TupleBuilder;
import net.sourceforge.mayfly.evaluation.condition.Equal;
import net.sourceforge.mayfly.evaluation.condition.IsNull;
import net.sourceforge.mayfly.evaluation.condition.Not;
import net.sourceforge.mayfly.evaluation.condition.Or;
import net.sourceforge.mayfly.evaluation.expression.SingleColumn;
import net.sourceforge.mayfly.evaluation.expression.literal.QuotedString;
import net.sourceforge.mayfly.parser.Parser;
import net.sourceforge.mayfly.util.MayflyAssert;

public class OrTest extends TestCase {

    public void testParse() throws Exception {
        Or outer = (Or) 
            new Parser("name='steve' or species='homo sapiens' or size = 6")
                .parseCondition().asBoolean();
            Or inner = (Or) outer.leftSide;
                Equal name = (Equal) inner.leftSide;
                    MayflyAssert.assertColumn("name", name.leftSide);
                    MayflyAssert.assertString("steve", name.rightSide);
                Equal species = (Equal) inner.rightSide;
                    MayflyAssert.assertColumn("species", species.leftSide);
                    MayflyAssert.assertString("homo sapiens", species.rightSide);
            Equal size = (Equal) outer.rightSide;
                MayflyAssert.assertColumn("size", size.leftSide);
                MayflyAssert.assertInteger(6, size.rightSide);
    }

    public void testEvaluate() throws Exception {
        Row row =
            new TupleBuilder()
                .append("x", new StringCell("foo"))
                .asRow()
        ;
        Equal compareWithFoo = new Equal(new SingleColumn("x"), new QuotedString("'foo'"));
        Equal compareWithXxx = new Equal(new SingleColumn("x"), new QuotedString("'xxx'"));
        Not notNull = new Not(new IsNull(new SingleColumn("x")));
        assertTrue(new Or(compareWithFoo, notNull).evaluate(row, "table1"));
        assertTrue(new Or(compareWithFoo, compareWithXxx).evaluate(row, "table1"));
        assertTrue(new Or(compareWithXxx, notNull).evaluate(row, "table1"));
        assertFalse(new Or(compareWithXxx, compareWithXxx).evaluate(row, "table1"));
    }

}
